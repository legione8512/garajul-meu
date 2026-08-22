import '@testing-library/jest-dom/vitest'

// Initialises i18next once for the whole suite, so a component test renders
// real translations rather than raw keys.
import '../i18n/config.ts'

import { configure } from '@testing-library/dom'
import { cleanup } from '@testing-library/react'
import { afterEach, beforeEach, vi } from 'vitest'

import i18n from '../i18n/config.ts'

/**
 * Testing Library waits one second for `findBy*` by default, and that default
 * was chosen for an unloaded desktop.
 *
 * On 2026-08-22 and again on 2026-08-23, three or four tests failed on a busy
 * run - always the first test in a file, which pays the module-loading cost,
 * and always still showing `Se încarcă…`. Every one passed on a quiet run. The
 * second occurrence was under `--coverage`, which instruments every module and
 * makes every future coverage run slower than the one that already failed. CI
 * machines are slower and shared, so this would have arrived there next.
 *
 * Raising the wait weakens nothing. These assertions say *eventually*, never
 * *within one second*, and not one of them measures performance - a component
 * that took four seconds would be a bug no test here is looking for. What the
 * wait actually buys is the diagnosis: a `findBy*` that gives up prints the DOM
 * it was searching, and Vitest's own test timeout prints nothing useful at all.
 * Which is why `testTimeout` in vite.config.ts is set well above this one.
 */
configure({ asyncUtilTimeout: 5_000 })

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