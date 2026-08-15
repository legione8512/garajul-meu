import { useTranslation } from 'react-i18next'

/** Screen 6 in specification section 5. Content arrives in 6.2. */
export function DashboardPage() {
  const { t } = useTranslation()

  return <h1>{t('screens.dashboard')}</h1>
}