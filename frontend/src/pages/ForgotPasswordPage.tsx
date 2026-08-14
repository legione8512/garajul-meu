import { useTranslation } from 'react-i18next'

/** Screen 5 in specification section 5, first half. */
export function ForgotPasswordPage() {
  const { t } = useTranslation()

  return <h1>{t('screens.forgotPassword')}</h1>
}