import '@testing-library/jest-dom/vitest'

import { cleanup } from '@testing-library/react'
import { afterEach, beforeEach } from 'vitest'

import i18n from '../i18n/config.ts'

/**
 * jsdom reports en-US, so without pinning the language every component test
 * would run in English and a Romanian assertion would fail for a reason that
 * has nothing to do with the component. Storage is cleared afterwards, so a
 * language one test switches to cannot leak into the next.
 */
beforeEach(async () => {
  await i18n.changeLanguage('ro')
  localStorage.clear()
})

// Testing Library only cleans up by itself when Vitest globals are enabled, and
// they are deliberately not. Without this, one test's DOM survives into the
// next and a query that should find one element finds two.
afterEach(() => {
  cleanup()
})