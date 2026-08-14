/**
 * The address a previous screen handed over, if there was one.
 *
 * <p>Route state rather than a query parameter. An email address in a URL lands
 * in browser history and in the access log of every proxy between here and the
 * server, which is exactly where personal data should not be. State is invisible
 * to both.
 *
 * <p>It is also lost on reload, which is why every screen that reads this still
 * renders an ordinary editable field: the handover is a convenience, never the
 * only way to supply the address.
 */
export function carriedEmail(state: unknown): string {
  if (typeof state !== 'object' || state === null || !('email' in state)) {
    return ''
  }

  const value = (state as { email: unknown }).email

  return typeof value === 'string' ? value : ''
}