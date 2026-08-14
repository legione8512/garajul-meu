import { apiFetch } from '../client.ts'
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
    body: JSON.stringify({ email, password }),
  })

  setAccessToken(result.accessToken)
}

/**
 * The token is cleared whether or not the server answered. Someone who asked to
 * sign out must end up signed out locally even if the network refused - the
 * server side is idempotent and the cookie expires on its own.
 */
export async function logout(): Promise<void> {
  try {
    await apiFetch<void>('/api/v1/auth/logout', { method: 'POST', body: '{}' })
  } finally {
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