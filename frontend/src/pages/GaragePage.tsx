import { useTranslation } from 'react-i18next'

/** Screen 7 in specification section 5. Vehicles arrive in Phase 7. */
export function GaragePage() {
  const { t } = useTranslation()

  return (
    <>
      <h1>{t('screens.garage')}</h1>
      <p>{t('garage.empty')}</p>
    </>
  )
}