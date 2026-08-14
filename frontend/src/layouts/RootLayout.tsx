import { useTranslation } from 'react-i18next'
import { Link, Outlet } from 'react-router'

import { LanguageSwitcher } from '../components/LanguageSwitcher.tsx'
import { paths } from '../routes/paths.ts'

/**
 * The frame every page renders inside.
 *
 * One <header> and one <main> give assistive technology something to jump
 * between, and each page owns the single <h1>, so the document never has two
 * competing top-level headings.
 */
export function RootLayout() {
  const { t } = useTranslation()

  return (
    <>
      <header>
        <Link to={paths.welcome}>{t('app.name')}</Link>
        <LanguageSwitcher />
      </header>
      <main>
        <Outlet />
      </main>
    </>
  )
}