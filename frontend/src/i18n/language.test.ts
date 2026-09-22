import { afterEach, describe, expect, it } from 'vitest'

import {
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

  it('starts in the language this device was told to use', () => {
    rememberLanguage('en')

    expect(initialLanguage()).toBe('en')
  })

  /**
   * The environment asks for English - jsdom's navigator reports en-US - which
   * is exactly the phone the application is tested on. Until 2026-09-22 this
   * started in English, and the instructions given to Apple and Google said it
   * starts in Romanian.
   */
  it('starts in Romanian whatever the phone or the browser asks for', () => {
    expect(navigator.language.startsWith('en')).toBe(true)

    expect(initialLanguage()).toBe('ro')
  })
})