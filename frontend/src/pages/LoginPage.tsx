import { useTranslation } from 'react-i18next'

/** Screen 4 in specification section 5. The form arrives in 5.4. */
export function LoginPage() {
  const { t } = useTranslation()

  return <h1>{t('screens.login')}</h1>
}