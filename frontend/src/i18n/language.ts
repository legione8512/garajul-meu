export const supportedLanguages = ['ro', 'en'] as const

export type SupportedLanguage = (typeof supportedLanguages)[number]

/** Where the choice made before there is an account is remembered. */
export const languageStorageKey = 'garajul-meu.language'

/**
 * Each language named in itself, never translated.
 *
 * Somebody who has landed in a language they cannot read must still be able to
 * find their own in the list - which they cannot do if "Romanian" is currently
 * rendered as a Romanian word they do not recognise.
 */
export const languageNames: Record<SupportedLanguage, string> = {
  ro: 'Română',
  en: 'English',
}

export function isSupportedLanguage(value: string | null): value is SupportedLanguage {
  return value !== null && (supportedLanguages as readonly string[]).includes(value)
}

export function rememberedLanguage(): SupportedLanguage | null {
  try {
    const stored = localStorage.getItem(languageStorageKey)
    return isSupportedLanguage(stored) ? stored : null
  } catch {
    // Storage can be unavailable - private browsing, blocked site data. Having
    // no remembered preference is an ordinary state, not a failure.
    return null
  }
}

export function rememberLanguage(language: SupportedLanguage): void {
  try {
    localStorage.setItem(languageStorageKey, language)
  } catch {
    // The switch still applies to this session; it simply will not survive a
    // reload. Refusing to change language because storage is blocked would be
    // a worse answer than forgetting the choice later.
  }
}

/**
 * Romanian, unless this device has been told otherwise.
 *
 * <p>A remembered choice wins, because someone who has switched language once
 * has said what they want. Nothing else is consulted - not the phone's
 * language, not the browser's.
 *
 * <p><strong>Until 2026-09-22 the browser's language came second</strong>, and
 * a phone set to English - a great many Romanian phones are - opened this
 * Romanian application in English: the Moto G6 Plus it is tested on did, and so,
 * very likely, did the devices Apple and Google review it on, whose sign-in
 * instructions say it is in Romanian by default. Changed at the developer's
 * request, on the web and in the application alike. The application is
 * Romanian-first - its listing, the documents it is about, and the backend's
 * default for a new account - and the language switcher is on every screen,
 * the sign-in screen included, for anybody who wants English. An account's own
 * language still takes over at sign-in (`AuthProvider`), so nobody who chose
 * English on their account loses it.
 */
export function initialLanguage(): SupportedLanguage {
  return rememberedLanguage() ?? 'ro'
}