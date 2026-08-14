import '@testing-library/jest-dom/vitest'

// Initialises i18next once for the whole suite, so a component test renders
// real translations rather than raw keys.
import '../i18n/config.ts'

import { cleanup } from '@testing-library/react'
import { afterEach, beforeEach, vi } from 'vitest'

import i18n from '../i18n/config.ts'

/**
 * jsdom reports en-US, so without pinning the language every component test
 * would run in English and a Romanian assertion would fail for a reason that
 * has nothing to do with the component.
 *
 * The fetch stub matters just as much. Any test that renders a route now mounts
 * AuthProvider, which attempts a silent refresh on mount; without a stub that
 * would be a real request to a backend that may not be running. Answering 401
 * means every test starts signed out, and a test that needs a session stubs
 * fetch itself.
 */
beforeEach(async () => {
  await i18n.changeLanguage('ro')
  localStorage.clear()

  vi.stubGlobal('fetch', vi.fn(() => Promise.resolve(
    new Response(JSON.stringify({ code: 'REFRESH_TOKEN_INVALID' }), {
      status: 401,
      headers: { 'Content-Type': 'application/json' },
    }),
  )))
})

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})