import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  test: {
    // Unit tests live in `src`, and only there. Vitest's default pattern
    // sweeps the whole project for `*.spec.*`, which since 14.1 means it also
    // collects `e2e/journey.spec.ts` and fails it with "Playwright Test did not
    // expect test() to be called here" - two test runners fighting over one
    // file. Naming the directory settles it once, rather than excluding each
    // Playwright file as it is written.
    //
    // Both suffixes, so a `.spec.ts` written inside `src` by habit still runs.
    // The failure mode being avoided here is the quiet one: a test file that
    // matches no pattern does not fail, it simply never runs.
    include: ['src/**/*.{test,spec}.{ts,tsx}'],

    // jsdom rather than the default node environment: these are component
    // tests and they need a DOM to render into.
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    // Globals stay off. Importing describe/it/expect costs one line per file
    // and keeps the test API visible instead of ambient - the same reason the
    // backend never relies on static imports nobody can trace.
    globals: false,
    restoreMocks: true,
  },
})