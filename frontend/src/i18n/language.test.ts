import { afterEach, describe, expect, it } from 'vitest'

import {
  browserLanguage,
  initialLanguage,
  languageStorageKey,
  rememberLanguage,
  rememberedLanguage,
} from './language.ts'

afterEach(() => {
  localStorage.clear()
})

describe('language preference', () => {
  it('reads back a language it remembered', () => {
    rememberLanguage('en')

    expect(rememberedLanguage()).toBe('en')
  })

  it('ignores a stored value that is not a language this application supports', () => {
    localStorage.setItem(languageStorageKey, 'fr')

    expect(rememberedLanguage()).toBeNull()
  })

  it('prefers the remembered choice over what the browser asks for', () => {
    rememberLanguage('en')

    expect(initialLanguage(['ro-RO'])).toBe('en')
  })

  it('takes the first language the browser asks for that we actually have', () => {
    expect(browserLanguage(['fr-FR', 'en-GB', 'ro'])).toBe('en')
  })

  it('falls back to Romanian when the browser asks for neither', () => {
    expect(browserLanguage(['fr-FR', 'de-DE'])).toBe('ro')
  })
})