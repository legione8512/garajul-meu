/**
 * How the refresh credential travels, and who owns it between requests.
 *
 * <p>Section 14 lets the same two endpoints answer on either channel and forbids
 * a client-type header, because the request itself already says how the client
 * works - `AuthController.refresh` reads the body first and falls back to the
 * cookie. This interface is the frontend half of that sentence.
 *
 * <p><strong>A browser must never own the token; a native client has no choice
 * but to.</strong> On the web it is an HttpOnly cookie the page cannot read,
 * which is the whole reason `tokenStore` keeps the access token in a module
 * variable rather than beside it in storage. Inside a Capacitor WebView that
 * cookie is dead by construction: the origin is `https://localhost` and the API
 * is `api.cyber-half.com` - different *sites*, so a `SameSite=Strict` cookie is
 * never attached, no matter what the request asks for.
 *
 * <p>Four members rather than three verbs, because what differs between the two
 * clients is not *how* a request is made - that stays in `refresh.ts` and
 * `client.ts`, written once - but **who holds the credential in between**.
 */
export interface SessionChannel {
  /**
   * Whether this client carries the refresh token itself, which is exactly what
   * `LoginRequest.refreshTokenInBody` asks for. Only `/login` needs telling: at
   * that point the client has presented nothing, so the backend cannot infer the
   * channel. On `/refresh` and `/logout` it can, and does.
   */
  readonly carriesTokenItself: boolean

  /** The token to present, or null when the cookie carries it. */
  present(): Promise<string | null>

  /**
   * Takes the token the server has just issued.
   *
   * <p><strong>Called before the new session is used, and that order is the
   * entire safety argument.</strong> The backend rotates on every refresh and
   * treats a spent token as theft - `RefreshTokenService.rotate` revokes the
   * whole family - so a native client that used the new session before writing
   * the new token would, if the process died in between, come back holding a
   * spent one and sign the account out of every device it owns. Persisting first
   * shrinks that window to the width of the write. It does not close it, and the
   * residual risk is recorded in the known-issues table with its trigger.
   */
  remember(refreshToken: string | null): Promise<void>

  /** Logout, and any refresh that failed: whatever was held is worthless now. */
  forget(): Promise<void>
}