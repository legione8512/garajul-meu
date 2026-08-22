import { useTranslation } from 'react-i18next'

/**
 * Screen 20 in specification section 5. A placeholder for the same reason as
 * screen 19, and with more at stake: section 24 fixes retention and deletion
 * rules that the final notice has to describe accurately, and section 35 has
 * not settled the retention periods yet.
 *
 * <p>What the application already does is stated, because it is true today and
 * verifiable: certificate owner and user details are optional and never used
 * for reminders, deletion is permanent, and photographs go with the vehicle
 * they belong to. None of that substitutes for the legal notice.
 */
export function PrivacyPage() {
  const { t } = useTranslation()

  return (
    <>
      <h1>{t('screens.privacy')}</h1>
      <p role="note">{t('legal.placeholder')}</p>
      <p>{t('legal.privacyScope')}</p>
    </>
  )
}