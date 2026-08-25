import { API_BASE_URL } from './config.ts'
import { sessionChannel } from './session/channel.ts'
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
 * server, however many were waiting. On a native client this matters more
 * rather than less: there the spent token would also have been written to the
 * device, so the next launch would present it again.
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

/**
 * There is no usable session any more, whatever the reason.
 *
 * <p>The store is cleared first and its failure is swallowed, because a device
 * that cannot clear its own storage must still end up signed out. The worst
 * case left behind is a stale token the backend has already refused.
 */
async function endSession(): Promise<false> {
  try {
    await sessionChannel.forget()
  } catch {
    // Deliberately silent: see above.
  }

  setAccessToken(null)
  return false
}

async function attemptRefresh(): Promise<boolean> {
  try {
    // Null on the web, where the cookie carries it; the stored token on a
    // native client. Read before the request rather than inside the body
    // expression, so a store that refuses fails here and not halfway through
    // building a request.
    const presented = await sessionChannel.present()

    const response = await fetch(`${API_BASE_URL}/api/v1/auth/refresh`, {
      method: 'POST',
      // Harmless and unused on a native client, which has no cookie jar; on the
      // web this is what sends the HttpOnly refresh cookie across the origin
      // boundary section 21 describes.
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      // An empty body makes the backend take the cookie channel and answer on
      // it. A token here makes it take the body channel and answer on that,
      // which is the native path - one endpoint, two channels, no header, as
      // section 14 requires.
      body: presented === null ? '{}' : JSON.stringify({ refreshToken: presented }),
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
      return await endSession()
    }

    const body = (await response.json()) as {
      accessToken?: string
      refreshToken?: string | null
    }

    if (typeof body.accessToken !== 'string') {
      return await endSession()
    }

    // Before the access token, never after. The ordering is the safety
    // property described on SessionChannel.remember: a native client that used
    // the new session before writing the new token would come back from a crash
    // holding a spent one, and a spent token revokes the whole family. A no-op
    // on the web, where the server has already replaced the cookie itself.
    await sessionChannel.remember(body.refreshToken ?? null)

    setAccessToken(body.accessToken)
    return true
  } catch {
    // The network failed, the timeout fired, the response was not JSON, or the
    // store refused the token it was handed. Either way there is no usable
    // session; treating it as one would loop.
    return await endSession()
  }
}