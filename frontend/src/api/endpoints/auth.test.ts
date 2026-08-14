import { afterEach, describe, expect, it, vi } from 'vitest'

import { setAccessToken } from '../tokenStore.ts'
import { register, resetPassword, verifyEmail } from './auth.ts'

afterEach(() => {
  vi.unstubAllGlobals()
  setAccessToken(null)
})

/**
 * The recorded arguments are read through these rather than indexed directly.
 * A zero-argument mock is typed as an empty tuple, so calls[0][0] does not
 * exist as far as TypeScript is concerned even though it is there at runtime.
 */
function urlOf(call: unknown[]): string {
  return String(call[0])
}

function bodyOf(call: unknown[]): unknown {
  return JSON.parse((call[1] as RequestInit).body as string)
}

describe('authentication endpoints', () => {
  /** 201 with an empty body - the case that used to make parse() throw. */
  it('registers with the fields the backend expects', async () => {
    const fetchMock = vi.fn(() => Promise.resolve(new Response(null, { status: 201 })))
    vi.stubGlobal('fetch', fetchMock)

    await register('Marius Robert', 'marius@example.com', 'a-long-enough-password', 'ro')

    expect(urlOf(fetchMock.mock.calls[0])).toContain('/api/v1/auth/register')
    expect(bodyOf(fetchMock.mock.calls[0])).toEqual({
      fullName: 'Marius Robert',
      email: 'marius@example.com',
      password: 'a-long-enough-password',
      preferredLanguage: 'ro',
    })
  })

  it('sends the address alongside the verification code', async () => {
    const fetchMock = vi.fn(() => Promise.resolve(new Response(null, { status: 204 })))
    vi.stubGlobal('fetch', fetchMock)

    await verifyEmail('marius@example.com', '123456')

    expect(bodyOf(fetchMock.mock.calls[0])).toEqual({ email: 'marius@example.com', code: '123456' })
  })

  it('sends the address, the code and the new password when resetting', async () => {
    const fetchMock = vi.fn(() => Promise.resolve(new Response(null, { status: 204 })))
    vi.stubGlobal('fetch', fetchMock)

    await resetPassword('marius@example.com', '123456', 'a-replacement-password')

    expect(bodyOf(fetchMock.mock.calls[0])).toEqual({
      email: 'marius@example.com',
      code: '123456',
      newPassword: 'a-replacement-password',
    })
  })
})