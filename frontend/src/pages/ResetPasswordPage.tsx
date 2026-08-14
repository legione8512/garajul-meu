import { useTranslation } from 'react-i18next'

/** Screen 5 in specification section 5, second half. */
export function ResetPasswordPage() {
  const { t } = useTranslation()

  return <h1>{t('screens.resetPassword')}</h1>
}