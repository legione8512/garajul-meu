import { afterEach, describe, expect, it, vi } from 'vitest'

import { ApiError } from './ApiError.ts'
import { apiFetch } from './client.ts'
import { setAccessToken } from './tokenStore.ts'

afterEach(() => {
  vi.unstubAllGlobals()
  setAccessToken(null)
})

function jsonResponse(status: number, body: unknown, headers: Record<string, string> = {}): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json', ...headers },
  })
}

function headersOf(call: unknown[]): Headers {
  return new Headers((call[1] as RequestInit).headers)
}

describe('api client', () => {
  it('sends the access token when there is one', async () => {
    setAccessToken('a-token')
    const fetchMock = vi.fn(() => Promise.resolve(jsonResponse(200, { ok: true })))
    vi.stubGlobal('fetch', fetchMock)

    await apiFetch('/api/v1/users/me')

    expect(headersOf(fetchMock.mock.calls[0]).get('Authorization')).toBe('Bearer a-token')
  })

  it('sends no authorization header when there is no token', async () => {
    const fetchMock = vi.fn(() => Promise.resolve(jsonResponse(200, { ok: true })))
    vi.stubGlobal('fetch', fetchMock)

    await apiFetch('/api/v1/users/me')

    expect(headersOf(fetchMock.mock.calls[0]).has('Authorization')).toBe(false)
  })

  it('refreshes once on a 401 and retries the request with the new token', async () => {
    setAccessToken('expired')
    let protectedCalls = 0

    const fetchMock = vi.fn((input: string) => {
      if (input.includes('/auth/refresh')) {
        return Promise.resolve(jsonResponse(200, { accessToken: 'fresh', expiresInSeconds: 600, refreshToken: null }))
      }

      protectedCalls += 1
      return Promise.resolve(protectedCalls === 1
        ? jsonResponse(401, { code: 'AUTHENTICATION_REQUIRED' })
        : jsonResponse(200, { fullName: 'Marius Robert' }))
    })
    vi.stubGlobal('fetch', fetchMock)

    await expect(apiFetch<{ fullName: string }>('/api/v1/users/me'))
      .resolves.toEqual({ fullName: 'Marius Robert' })

    expect(protectedCalls).toBe(2)
  })

  /**
   * The property the whole design rests on. Two panels expiring together must
   * not each spend the refresh token, because the second one would be
   * presenting a token the first already used - which the backend treats as
   * theft and answers by ending every session the account has.
   */
  it('collapses two simultaneous 401s into a single refresh', async () => {
    setAccessToken('expired')
    let refreshCalls = 0
    let protectedCalls = 0

    const fetchMock = vi.fn((input: string) => {
      if (input.includes('/auth/refresh')) {
        refreshCalls += 1
        return Promise.resolve(jsonResponse(200, { accessToken: 'fresh', expiresInSeconds: 600, refreshToken: null }))
      }

      protectedCalls += 1
      // The first two calls are the two originals; anything after is a retry.
      return Promise.resolve(protectedCalls <= 2
        ? jsonResponse(401, { code: 'AUTHENTICATION_REQUIRED' })
        : jsonResponse(200, { ok: true }))
    })
    vi.stubGlobal('fetch', fetchMock)

    await Promise.all([apiFetch('/api/v1/users/me'), apiFetch('/api/v1/vehicles')])

    expect(refreshCalls).toBe(1)
    expect(protectedCalls).toBe(4)
  })

  it('does not refresh when the login endpoint itself answers 401', async () => {
    const fetchMock = vi.fn(() => Promise.resolve(jsonResponse(401, { code: 'INVALID_CREDENTIALS' })))
    vi.stubGlobal('fetch', fetchMock)

    await expect(apiFetch('/api/v1/auth/login', { method: 'POST', body: '{}' }))
      .rejects.toBeInstanceOf(ApiError)

    expect(fetchMock).toHaveBeenCalledTimes(1)
  })

  it('turns a failure into an ApiError carrying the code and the request id', async () => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.resolve(
      jsonResponse(409, { code: 'EMAIL_ALREADY_EXISTS', requestId: 'abc-123', fieldErrors: [] }),
    )))

    const error = await apiFetch('/api/v1/users/me').catch((thrown: unknown) => thrown)

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).code).toBe('EMAIL_ALREADY_EXISTS')
    expect((error as ApiError).requestId).toBe('abc-123')
  })

  it('still produces an ApiError when the response carries no JSON at all', async () => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.resolve(
      new Response('<html>gateway error</html>', { status: 502 }),
    )))

    const error = await apiFetch('/api/v1/users/me').catch((thrown: unknown) => thrown)

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).code).toBe('UNKNOWN')
  })

  it('returns nothing for a 204 rather than trying to parse an empty body', async () => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.resolve(new Response(null, { status: 204 }))))

    await expect(apiFetch<void>('/api/v1/users/me/change-password', { method: 'POST', body: '{}' }))
      .resolves.toBeUndefined()
  })
})