import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'

import { initialLanguage, isSupportedLanguage, rememberLanguage, supportedLanguages } from './language.ts'
import { en } from './locales/en.ts'
import { ro } from './locales/ro.ts'

void i18n.use(initReactI18next).init({
  resources: {
    ro: { translation: ro },
    en: { translation: en },
  },
  lng: initialLanguage(),
  supportedLngs: [...supportedLanguages],
  fallbackLng: 'ro',
  interpolation: {
    // React already escapes what it renders. Escaping twice turns an apostrophe
    // into &#39; on screen.
    escapeValue: false,
  },
})

// Persisting on the event rather than inside a switcher component means any
// future caller - including the post-login sync with users.preferred_language
// in 5.3 - is remembered too, without having to know it should be.
i18n.on('languageChanged', (language) => {
  if (isSupportedLanguage(language)) {
    rememberLanguage(language)
    document.documentElement.lang = language
  }
})

// index.html ships `lang="ro"`, which is right for the served default and wrong
// the moment the browser asked for English. The event above does not fire for
// the language `init` started with, so the two are reconciled here.
//
// This is not decoration: `lang` is what tells a screen reader which voice to
// use, what stops the browser offering to translate a page already in the
// reader's language, and what hyphenation and quotation rules follow.
document.documentElement.lang = i18n.language

export default i18n