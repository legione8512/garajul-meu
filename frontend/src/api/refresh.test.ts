import { afterEach, describe, expect, it, vi } from 'vitest'

import { refreshSession } from './refresh.ts'
import { getAccessToken, setAccessToken } from './tokenStore.ts'

afterEach(() => {
  vi.unstubAllGlobals()
  vi.useRealTimers()
  setAccessToken(null)
})

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('session refresh', () => {
  it('stores the new access token', async () => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.resolve(
      jsonResponse(200, { accessToken: 'fresh', expiresInSeconds: 600, refreshToken: null }),
    )))

    await expect(refreshSession()).resolves.toBe(true)
    expect(getAccessToken()).toBe('fresh')
  })

  /**
   * The reason this module exists. The backend revokes an entire token family
   * when a spent refresh token is presented, so a second concurrent refresh
   * would sign the user out of every device.
   */
  it('makes one request no matter how many callers ask at once', async () => {
    const fetchMock = vi.fn(() => Promise.resolve(
      jsonResponse(200, { accessToken: 'fresh', expiresInSeconds: 600, refreshToken: null }),
    ))
    vi.stubGlobal('fetch', fetchMock)

    await Promise.all([refreshSession(), refreshSession(), refreshSession()])

    expect(fetchMock).toHaveBeenCalledTimes(1)
  })

  it('lets a later expiry refresh again rather than reusing the first result', async () => {
    const fetchMock = vi.fn(() => Promise.resolve(
      jsonResponse(200, { accessToken: 'fresh', expiresInSeconds: 600, refreshToken: null }),
    ))
    vi.stubGlobal('fetch', fetchMock)

    await refreshSession()
    await refreshSession()

    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('clears the token when the refresh is refused', async () => {
    setAccessToken('stale')
    vi.stubGlobal('fetch', vi.fn(() => Promise.resolve(
      jsonResponse(401, { code: 'REFRESH_TOKEN_INVALID' }),
    )))

    await expect(refreshSession()).resolves.toBe(false)
    expect(getAccessToken()).toBeNull()
  })

  it('clears the token when the network fails outright', async () => {
    setAccessToken('stale')
    vi.stubGlobal('fetch', vi.fn(() => Promise.reject(new Error('offline'))))

    await expect(refreshSession()).resolves.toBe(false)
    expect(getAccessToken()).toBeNull()
  })

  /**
   * The Huawei MediaPad T3 of 2026-09-19: Android 7.0, a WebView stuck at
   * Chrome 101, and `AbortSignal.timeout` arrived in 103. Calling it there threw
   * before any request left, the `catch` read that as a failed refresh, and
   * `endSession()` forgot the stored token - so the person was signed out at
   * every launch. The engine is made to lack the function here, as that one
   * does, and a refresh must still succeed.
   */
  it('refreshes on an engine without AbortSignal.timeout, as Chrome 101 is', async () => {
    const original = Object.getOwnPropertyDescriptor(AbortSignal, 'timeout')
    Reflect.deleteProperty(AbortSignal, 'timeout')

    try {
      // The premise, checked: had the deletion failed, this test would pass for
      // the wrong reason.
      expect(AbortSignal.timeout).toBeUndefined()
      vi.stubGlobal('fetch', vi.fn(() => Promise.resolve(
        jsonResponse(200, { accessToken: 'fresh', expiresInSeconds: 600, refreshToken: null }),
      )))

      await expect(refreshSession()).resolves.toBe(true)
      expect(getAccessToken()).toBe('fresh')
    } finally {
      if (original !== undefined) {
        Object.defineProperty(AbortSignal, 'timeout', original)
      }
    }
  })

  /**
   * What the bound is for, and what replacing `AbortSignal.timeout` must not
   * lose: a refresh that is never answered ends rather than holding the slot
   * for the life of the page. Twenty seconds is `REFRESH_TIMEOUT_MS`.
   */
  it('still gives up on a refresh nobody answers', async () => {
    vi.useFakeTimers()
    setAccessToken('stale')
    vi.stubGlobal('fetch', vi.fn((_url: string, init?: RequestInit) => new Promise<Response>((_resolve, reject) => {
      init?.signal?.addEventListener('abort', () => {
        reject(new DOMException('The operation was aborted.', 'AbortError'))
      })
    })))

    const outcome = refreshSession()
    await vi.advanceTimersByTimeAsync(20_000)

    await expect(outcome).resolves.toBe(false)
    expect(getAccessToken()).toBeNull()
  })
})
