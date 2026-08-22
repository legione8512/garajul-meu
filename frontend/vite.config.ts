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

    // Comfortably above the 5s `asyncUtilTimeout` set in src/test/setup.ts, and
    // that ordering is the point rather than the number. Whichever limit trips
    // first is the one that reports, and only Testing Library prints the DOM it
    // was searching - Vitest just says the test ran out of time. Leaving the
    // default 5s here would let the two race and turn a readable failure into
    // an unreadable one. Nothing is expected to take this long; a real hang
    // still stops.
    testTimeout: 15_000,

    // jsdom rather than the default node environment: these are component
    // tests and they need a DOM to render into.
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    // Globals stay off. Importing describe/it/expect costs one line per file
    // and keeps the test API visible instead of ambient - the same reason the
    // backend never relies on static imports nobody can trace.
    globals: false,
    restoreMocks: true,

    coverage: {
      provider: 'v8',
      reporter: ['text', 'html'],

      // Naming `src` rather than leaving this to the default is the point of
      // the whole exercise. By default only files a test imported are counted,
      // so a module nobody tests is not reported as untested - it is not
      // reported at all, and the percentage looks better for its absence. This
      // way an untouched file appears at 0%, which is the number that actually
      // tells you something.
      include: ['src/**/*.{ts,tsx}'],

      // Excluded so that a 0% row always means something. A row that can never
      // improve is worse than no row: it teaches whoever reads this report to
      // skip past zeroes, which is the one number here worth stopping at.
      exclude: [
        'src/**/*.{test,spec}.{ts,tsx}',
        'src/test/**',
        'src/**/*.d.ts',
        // Declares one interface and nothing else. TypeScript erases it, so
        // there is no runtime code to cover - the 0% it reported was 0 of 0.
        'src/api/page.ts',
        // Six lines wrapping AppRoutes in a BrowserRouter, and its own comment
        // says why no test touches it: BrowserRouter reads the real address bar
        // and cannot be told to start somewhere else. Everything testable was
        // deliberately put in AppRoutes, which is at 100%.
        'src/App.tsx',
        // The bootstrap: it mounts React and does nothing a test could assert.
        'src/main.tsx',
      ],
    },
  },
})