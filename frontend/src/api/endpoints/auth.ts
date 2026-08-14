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
 * server side is idempotent and the cookie will expire on its own.
 */
export async function logout(): Promise<void> {
  try {
    await apiFetch<void>('/api/v1/auth/logout', { method: 'POST', body: '{}' })
  } finally {
    setAccessToken(null)
  }
}