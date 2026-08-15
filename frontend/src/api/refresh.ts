import { API_BASE_URL } from './config.ts'
import { setAccessToken } from './tokenStore.ts'

/**
 * Generous, because Neon scales to zero when idle and a cold start is slow.
 * The point is not to be strict; it is that the wait is bounded at all.
 */
const REFRESH_TIMEOUT_MS = 20_000

/**
 * The single refresh in flight, if there is one.
 *
 * <p>This variable is the whole point of the module. The backend rotates
 * refresh tokens and treats a spent one as theft: presenting it revokes the
 * entire family and ends the session on every device. Two requests that expire
 * together would each call /auth/refresh, the second still carrying the token
 * the first just spent, and the user would be signed out for doing nothing but
 * loading a page with two panels on it.
 *
 * <p>So every caller shares one promise. Exactly one request reaches the
 * server, however many were waiting.
 */
let inFlight: Promise<boolean> | null = null

export function refreshSession(): Promise<boolean> {
  inFlight ??= attemptRefresh().finally(() => {
    // Cleared so a later expiry can refresh again. Leaving it set would make
    // the first refresh the only one this page ever performs.
    inFlight = null
  })

  return inFlight
}

async function attemptRefresh(): Promise<boolean> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/v1/auth/refresh`, {
      method: 'POST',
      // The refresh token is an HttpOnly cookie; this is what sends it.
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      // An empty body, so the backend takes the cookie channel and answers on
      // it. Sending a token here would be the native-client path.
      body: '{}',
      // Without a bound, a request that is never answered - a network that
      // swallows rather than refuses - leaves the slot above occupied for the
      // life of the page. Every later expiry would then await a promise that
      // can never settle, and the session would appear frozen rather than
      // expired. Scoped to refresh deliberately: one stuck request here
      // disables refreshing for everything, while any other request failing
      // affects only itself.
      signal: AbortSignal.timeout(REFRESH_TIMEOUT_MS),
    })

    if (!response.ok) {
      setAccessToken(null)
      return false
    }

    const body = (await response.json()) as { accessToken?: string }

    if (typeof body.accessToken !== 'string') {
      setAccessToken(null)
      return false
    }

    setAccessToken(body.accessToken)
    return true
  } catch {
    // The network failed, the timeout fired, or the response was not JSON.
    // Either way there is no usable session; treating it as one would loop.
    setAccessToken(null)
    return false
  }
}