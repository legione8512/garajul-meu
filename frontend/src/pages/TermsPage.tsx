import { useTranslation } from 'react-i18next'

/**
 * Screen 19 in specification section 5.
 *
 * <p><strong>The content is a placeholder and says so on the page itself.</strong>
 * Section 24 makes this a release-blocking deliverable and section 35 defers the
 * final wording; a page carrying plausible-looking invented terms is the one
 * that ships by accident, because nothing about it looks unfinished. Wording
 * that announces itself as absent cannot be mistaken for wording that is done.
 *
 * <p>Deliberately not a link to a document elsewhere: the text has to be
 * versioned with the application that is bound by it.
 */
export function TermsPage() {
  const { t } = useTranslation()

  return (
    <>
      <h1>{t('screens.terms')}</h1>
      <p role="note">{t('legal.placeholder')}</p>
      <p>{t('legal.termsScope')}</p>
    </>
  )
}