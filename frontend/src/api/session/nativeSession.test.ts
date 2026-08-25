import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

/**
 * The safety property of the whole native session model, tested where it
 * actually lives: in the order `refresh.ts` does two things.
 *
 * <p>`vi.hoisted` is not decoration - `vi.mock` is hoisted above the imports, so
 * its factory cannot see an ordinary module-level variable. This is the shared
 * state both the fake store and the assertions need.
 */
const shared = vi.hoisted(() => ({ held: null as string | null, order: [] as string[] }))

vi.mock('./channel.ts', async () => {
  const { nativeSessionChannel } = await import('./nativeChannel.ts')

  return {
    sessionChannel: nativeSessionChannel({
      read: () => Promise.resolve(shared.held),
      write: (value: string) => {
        shared.held = value
        shared.order.push('written-to-device')
        return Promise.resolve()
      },
      clear: () => {
        shared.held = null
        return Promise.resolve()
      },
    }),
  }
})

const { refreshSession } = await import('../refresh.ts')
const { setAccessToken, subscribeToAccessToken } = await import('../tokenStore.ts')

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

/**
 * Declared with both parameters even though only the second is read.
 *
 * <p>`vi.fn` infers its call signature from the function it is handed, so a stub
 * written as `() => …` types every recorded call as an **empty tuple** - and
 * reading `calls[0][1]` off it is a type error rather than the `undefined` it
 * looks like. The first version of this file did exactly that, passed 258 green
 * tests, and failed `tsc`: Vitest transpiles without checking types, which is
 * why this project treats `tsc` as its own gate.
 *
 * <p>Capturing the body here sidesteps the tuple altogether.
 */
function stubFetch(response: Response, captured: { body?: string }): void {
  vi.stubGlobal('fetch', vi.fn((_input: RequestInfo | URL, init?: RequestInit) => {
    captured.body = init?.body as string
    return Promise.resolve(response)
  }))
}

beforeEach(() => {
  shared.held = null
  shared.order = []
})

afterEach(() => {
  vi.unstubAllGlobals()
  setAccessToken(null)
})

describe('refreshing on a native client', () => {
  it('presents the stored token in the body instead of relying on a cookie', async () => {
    shared.held = 'held-on-device'
    const captured: { body?: string } = {}
    stubFetch(
      jsonResponse(200, { accessToken: 'fresh', expiresInSeconds: 600, refreshToken: 'rotated' }),
      captured,
    )

    await expect(refreshSession()).resolves.toBe(true)

    expect(JSON.parse(captured.body ?? '{}')).toEqual({ refreshToken: 'held-on-device' })
  })

  /**
   * The one that matters. The backend rotates on every refresh and revokes the
   * whole family when a spent token reappears, so if the process died between
   * using the new session and writing the new token, the next launch would
   * present a spent one and sign the account out of every device it owns.
   *
   * <p>Observed through the token store's own subscription rather than by
   * spying, because that is the moment the new session actually becomes usable
   * to the rest of the application.
   */
  it('writes the rotated token before the new access token becomes usable', async () => {
    shared.held = 'about-to-be-spent'
    const stop = subscribeToAccessToken((token) => {
      if (token !== null) {
        shared.order.push('access-token-published')
      }
    })
    stubFetch(
      jsonResponse(200, { accessToken: 'fresh', expiresInSeconds: 600, refreshToken: 'rotated' }),
      {},
    )

    await refreshSession()
    stop()

    expect(shared.order).toEqual(['written-to-device', 'access-token-published'])
    expect(shared.held).toBe('rotated')
  })

  /**
   * A token the server has already refused must not survive on the device: every
   * later launch would spend a rate-limit slot presenting it and fail the same
   * way, and the person would meet an application that never signs in.
   */
  it('forgets the stored token when the server refuses it', async () => {
    shared.held = 'spent'
    stubFetch(jsonResponse(401, { code: 'REFRESH_TOKEN_INVALID' }), {})

    await expect(refreshSession()).resolves.toBe(false)
    expect(shared.held).toBeNull()
  })
})
