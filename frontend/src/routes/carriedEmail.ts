/**
 * Readers for the things one screen hands another through route state.
 *
 * <p>Route state rather than query parameters. An address in a URL lands in
 * browser history and in the access log of every proxy on the path, which is
 * where personal data should not be. State is invisible to both.
 *
 * <p>It is also lost on reload, which is why every screen that reads one of
 * these treats it as a convenience and never as the only source.
 */
export function carriedEmail(state: unknown): string {
  if (typeof state !== 'object' || state === null || !('email' in state)) {
    return ''
  }

  const value = (state as { email: unknown }).email

  return typeof value === 'string' ? value : ''
}

/**
 * Where a protected route turned somebody away from, so signing in can return
 * them there instead of dropping them on the dashboard.
 *
 * <p>The leading-slash check is not decoration. Without it, anything that could
 * put a value into route state could send a freshly authenticated person to
 * another origin - an open redirect, arriving through the one navigation the
 * application performs while holding a live session.
 */
export function returnTo(state: unknown): string | null {
  if (typeof state !== 'object' || state === null || !('from' in state)) {
    return null
  }

  const value = (state as { from: unknown }).from

  return typeof value === 'string' && value.startsWith('/') ? value : null
}