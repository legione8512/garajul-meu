import { useTranslation } from 'react-i18next'

/**
 * Screen 6 in specification section 5.
 *
 * <p>No call to action yet. Adding a vehicle requires a confirmed registration
 * certificate, which is Phase 8, and a button that leads nowhere is worse than
 * no button. The actions arrive with the flows that can finish them.
 */
export function DashboardPage() {
  const { t } = useTranslation()

  return (
    <>
      <h1>{t('screens.dashboard')}</h1>
      <p>{t('dashboard.empty')}</p>
    </>
  )
}