import { useTranslation } from 'react-i18next'

/** Screen 7 in specification section 5. Content arrives in 6.2. */
export function GaragePage() {
  const { t } = useTranslation()

  return <h1>{t('screens.garage')}</h1>
}