import { useTranslation } from 'react-i18next'

/**
 * Not one of the twenty-two screens in section 5, and still required: any
 * address the router does not recognise has to land somewhere deliberate.
 */
export function NotFoundPage() {
  const { t } = useTranslation()

  return <h1>{t('screens.notFound')}</h1>
}