type Listener = (token: string | null) => void

/**
 * The access token lives here and nowhere else - in a module variable, never in
 * localStorage or sessionStorage.
 *
 * <p>Any XSS can read storage; that is the whole reason the refresh token is an
 * HttpOnly cookie the page cannot touch. Putting the token that opens every
 * endpoint next to it in plain text would give back exactly what that design
 * bought.
 *
 * <p>The cost is that a page reload loses it, which is why the session is
 * restored by a silent refresh at startup rather than by reading it back.
 */
let accessToken: string | null = null

const listeners = new Set<Listener>()

export function getAccessToken(): string | null {
  return accessToken
}

export function setAccessToken(token: string | null): void {
  accessToken = token
  for (const listener of listeners) {
    listener(token)
  }
}

/**
 * Lets the authentication context notice a session ending in the background -
 * a refresh that failed behind some unrelated request - rather than only when
 * the user happens to click something.
 */
export function subscribeToAccessToken(listener: Listener): () => void {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}