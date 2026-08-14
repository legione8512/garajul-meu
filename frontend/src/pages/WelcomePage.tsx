import { useTranslation } from 'react-i18next'

/** Screen 1 in specification section 5. Content arrives in 5.4. */
export function WelcomePage() {
  const { t } = useTranslation()

  return <h1>{t('screens.welcome')}</h1>
}