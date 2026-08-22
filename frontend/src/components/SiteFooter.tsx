import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'

import { paths } from '../routes/paths.ts'

/**
 * The two documents section 24 calls release-blocking, reachable from every
 * screen on both sides of the sign-in boundary.
 *
 * <p>A privacy policy that can only be found from inside an account is not
 * reachable by the person deciding whether to create one, which is precisely
 * when it matters most.
 */
export function SiteFooter() {
  const { t } = useTranslation()

  return (
    <footer>
      <Link to={paths.terms}>{t('legal.terms')}</Link>
      {' '}
      <Link to={paths.privacy}>{t('legal.privacy')}</Link>
    </footer>
  )
}