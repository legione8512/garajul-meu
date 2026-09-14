import { useTranslation } from 'react-i18next'

import { LegalDocumentView } from '../legal/LegalDocumentView.tsx'
import { termsAndConditions } from '../legal/terms.ts'

/**
 * Screen 19 in specification section 5, the terms and conditions. The placeholder that stood here until 2026-09-14
 * announced itself as unfinished on purpose; the wording that replaced it is in
 * legal/terms.ts.
 */
export function TermsPage() {
  const { t } = useTranslation()

  return <LegalDocumentView title={t('screens.terms')} documents={termsAndConditions} />
}