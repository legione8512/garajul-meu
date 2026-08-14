import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'

import { useAuth } from '../auth/useAuth.ts'
import { paths } from '../routes/paths.ts'

/**
 * Screen 1 in specification section 5, and the first place the three-state
 * status earns itself: while the silent refresh is still in flight the page
 * offers neither a way in nor a greeting. Showing "sign in" during that moment
 * would tell somebody who is perfectly signed in that they are not.
 *
 * <p>Phase 6 replaces the authenticated half with the dashboard.
 */
export function WelcomePage() {
  const { t } = useTranslation()
  const { status, profile, signOut } = useAuth()

  return (
    <>
      <h1>{t('screens.welcome')}</h1>

      {status === 'anonymous' ? (
        <p>
          <Link to={paths.login}>{t('welcome.signIn')}</Link>
          {' · '}
          <Link to={paths.register}>{t('welcome.createAccount')}</Link>
        </p>
      ) : null}

      {status === 'authenticated' && profile !== null ? (
        <>
          <p>{t('welcome.signedInAs', { name: profile.fullName })}</p>
          <button type="button" onClick={() => { void signOut() }}>{t('welcome.signOut')}</button>
        </>
      ) : null}
    </>
  )
}