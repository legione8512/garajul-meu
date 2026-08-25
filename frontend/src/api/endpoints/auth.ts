import { apiFetch } from '../client.ts'
import { sessionChannel } from '../session/channel.ts'
import { setAccessToken } from '../tokenStore.ts'

interface LoginResponse {
  accessToken: string
  expiresInSeconds: number
  /** Null for a browser: the token travelled in the HttpOnly cookie instead. */
  refreshToken: string | null
}

export async function login(email: string, password: string): Promise<void> {
  const result = await apiFetch<LoginResponse>('/api/v1/auth/login', {
    method: 'POST',
    // The one request that has to say which client this is. At login nothing
    // has been presented yet, so the backend cannot infer the channel from the
    // request the way it can on /refresh and /logout. False and absent mean the
    // same thing to `LoginRequest.wantsRefreshTokenInBody`, so the web request
    // is unchanged in everything but a field the server already ignored.
    body: JSON.stringify({
      email,
      password,
      refreshTokenInBody: sessionChannel.carriesTokenItself,
    }),
  })

  // Before the access token, for the reason given on SessionChannel.remember.
  await sessionChannel.remember(result.refreshToken)

  setAccessToken(result.accessToken)
}

/**
 * The token is cleared whether or not the server answered. Someone who asked to
 * sign out must end up signed out locally even if the network refused - the
 * server side is idempotent and the cookie expires on its own.
 *
 * <p>On a native client the stored token is cleared too, and its own failure is
 * swallowed for the same reason: a store that will not clear must not be able to
 * keep somebody signed in.
 */
export async function logout(): Promise<void> {
  try {
    const presented = await sessionChannel.present()

    await apiFetch<void>('/api/v1/auth/logout', {
      method: 'POST',
      body: presented === null ? '{}' : JSON.stringify({ refreshToken: presented }),
    })
  } finally {
    try {
      await sessionChannel.forget()
    } catch {
      // Deliberately silent: see above.
    }

    setAccessToken(null)
  }
}

/**
 * The language is not a form field. Whatever the switcher currently shows is
 * what the account is created with, which is both the obvious guess and the one
 * the person can already see and change before submitting.
 */
export function register(
  fullName: string,
  email: string,
  password: string,
  preferredLanguage: string,
): Promise<void> {
  return apiFetch<void>('/api/v1/auth/register', {
    method: 'POST',
    body: JSON.stringify({ fullName, email, password, preferredLanguage }),
  })
}

export function verifyEmail(email: string, code: string): Promise<void> {
  return apiFetch<void>('/api/v1/auth/verify-email', {
    method: 'POST',
    body: JSON.stringify({ email, code }),
  })
}

export function resendVerification(email: string): Promise<void> {
  return apiFetch<void>('/api/v1/auth/resend-verification', {
    method: 'POST',
    body: JSON.stringify({ email }),
  })
}

export function forgotPassword(email: string): Promise<void> {
  return apiFetch<void>('/api/v1/auth/forgot-password', {
    method: 'POST',
    body: JSON.stringify({ email }),
  })
}

export function resetPassword(email: string, code: string, newPassword: string): Promise<void> {
  return apiFetch<void>('/api/v1/auth/reset-password', {
    method: 'POST',
    body: JSON.stringify({ email, code, newPassword }),
  })
}