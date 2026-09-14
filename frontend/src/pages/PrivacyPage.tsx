import { useTranslation } from 'react-i18next'

import { LegalDocumentView } from '../legal/LegalDocumentView.tsx'
import { privacyPolicy } from '../legal/privacy.ts'

/**
 * Screen 20 in specification section 5, the privacy notice section 24 requires. The placeholder that stood here
 * until 2026-09-14 announced itself as unfinished on purpose; the wording that
 * replaced it, and how it was arrived at, is in legal/privacy.ts.
 */
export function PrivacyPage() {
  const { t } = useTranslation()

  return <LegalDocumentView title={t('screens.privacy')} documents={privacyPolicy} />
}