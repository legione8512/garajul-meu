import { useTranslation } from 'react-i18next'
import { Link, Navigate } from 'react-router'

import { useAuth } from '../auth/useAuth.ts'
import { paths } from '../routes/paths.ts'

/**
 * Screen 1 in specification section 5: the public landing page.
 *
 * <p>Somebody already signed in has no use for it, so they are sent to the
 * dashboard. Keeping `/` public and `/dashboard` separate, rather than having
 * one address render two different pages, costs a redirect on arrival and buys
 * routes that can be read, tested and sent to somebody as a link.
 *
 * <p>While the status is unknown neither branch shows: offering "sign in" for
 * that moment would tell somebody who is signed in that they are not.
 */
export function WelcomePage() {
  const { t } = useTranslation()
  const { status } = useAuth()

  if (status === 'authenticated') {
    return <Navigate to={paths.dashboard} replace />
  }

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
    </>
  )
}