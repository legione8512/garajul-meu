import { useTranslation } from 'react-i18next'
import { Navigate, Outlet, useLocation } from 'react-router'

import { paths } from '../routes/paths.ts'
import { useAuth } from './useAuth.ts'

/**
 * The gate. Until now the authentication status only decided what a page said;
 * here it decides whether a page is reached at all.
 *
 * <p>The loading branch matters more than it looks. The silent refresh at
 * startup goes to the backend, which goes to Neon - and Neon scales to zero
 * when idle, so a cold start is slow. Rendering nothing during that would show
 * a blank page for seconds; redirecting to sign-in would be worse still, since
 * the person may well be signed in and we simply do not know yet.
 *
 * <p>role="status" so the wait is announced rather than only drawn.
 */
export function RequireAuth() {
  const { status } = useAuth()
  const { t } = useTranslation()
  const location = useLocation()

  if (status === 'unknown') {
    return <p role="status">{t('common.loading')}</p>
  }

  if (status === 'anonymous') {
    // replace, so the protected address does not sit in history as a back
    // target that would bounce straight back here. `from` lets signing in
    // return the person to what they actually asked for.
    return <Navigate to={paths.login} replace state={{ from: location.pathname }} />
  }

  return <Outlet />
}