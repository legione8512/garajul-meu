import { useTranslation } from 'react-i18next'

/** Screen 3 in specification section 5. The code entry arrives in 5.4. */
export function VerifyEmailPage() {
  const { t } = useTranslation()

  return <h1>{t('screens.verifyEmail')}</h1>
}