import { afterEach, describe, expect, it, vi } from 'vitest'

import { refreshSession } from './refresh.ts'
import { getAccessToken, setAccessToken } from './tokenStore.ts'

afterEach(() => {
  vi.unstubAllGlobals()
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
})