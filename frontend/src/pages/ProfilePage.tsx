import { useTranslation } from 'react-i18next'

/** Screen 15 in specification section 5, minimal. Settings are Phase 13. */
export function ProfilePage() {
  const { t } = useTranslation()

  return <h1>{t('screens.profile')}</h1>
}