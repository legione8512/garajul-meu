import { useTranslation } from 'react-i18next'

import { useOnline } from './useOnline.ts'

/**
 * Screen 21 in specification section 5, as a band rather than a page.
 *
 * <p>Section 25 asks for a clear offline state, not an offline destination.
 * Sending somebody to a dedicated screen would throw away whatever they had
 * typed, and they would have to find their way back; a band above the content
 * says the same thing and costs them nothing.
 *
 * <p>`role="status"` rather than `role="alert"`: losing the network is worth
 * announcing, and worth announcing politely - an assertive live region
 * interrupts whatever a screen reader is in the middle of saying, which for a
 * condition the person usually already knows about is rude rather than helpful.
 *
 * <p>Rendered in both layouts. Being signed out does not make the network any
 * more present, and the sign-in form is exactly where a silent failure is most
 * confusing.
 */
export function OfflineNotice() {
  const { t } = useTranslation()
  const online = useOnline()

  if (online) {
    return null
  }

  return <p role="status">{t('common.offline')}</p>
}