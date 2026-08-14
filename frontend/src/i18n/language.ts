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
 * Romanian unless the browser clearly asks for English.
 *
 * The candidate list is a parameter with a real default rather than a direct
 * read of navigator, so this stays a pure function: the tests pass it a list
 * instead of rewriting a global the whole environment shares.
 */
export function browserLanguage(
  candidates: readonly string[] = navigator.languages ?? [navigator.language],
): SupportedLanguage {
  for (const candidate of candidates) {
    // 'en-GB' and 'en-US' are both English as far as this application cares.
    const base = candidate.split('-')[0]
    if (isSupportedLanguage(base)) {
      return base
    }
  }

  return 'ro'
}

/**
 * A remembered choice outranks the browser, because someone who has switched
 * language once has said something more specific than their operating system
 * did. Romanian is the last word: the application is Romanian-first and the
 * backend defaults new accounts to RO, so an ambiguous case resolves the same
 * way on both sides rather than differently.
 */
export function initialLanguage(candidates?: readonly string[]): SupportedLanguage {
  return rememberedLanguage() ?? browserLanguage(candidates)
}