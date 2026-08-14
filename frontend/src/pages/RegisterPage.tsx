import { useTranslation } from 'react-i18next'

/** Screen 2 in specification section 5. The form arrives in 5.4. */
export function RegisterPage() {
  const { t } = useTranslation()

  return <h1>{t('screens.register')}</h1>
}