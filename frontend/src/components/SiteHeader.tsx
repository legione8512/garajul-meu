import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'

import { paths } from '../routes/paths.ts'
import { LanguageSwitcher } from './LanguageSwitcher.tsx'

/**
 * The same header on both sides of the sign-in boundary, so it exists once.
 * The two layouts differ in what surrounds it, not in it.
 */
export function SiteHeader() {
  const { t } = useTranslation()

  return (
    <header>
      <Link to={paths.welcome}>{t('app.name')}</Link>
      <LanguageSwitcher />
    </header>
  )
}