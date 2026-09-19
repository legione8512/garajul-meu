import { fileURLToPath } from 'node:url'

import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],

  resolve: {
    alias: {
      // @capacitor-firebase/messaging's web implementation imports this, and it
      // is an optional peer we do not install. Rollup follows the dynamic
      // import to it regardless of whether the branch can run, so without this
      // line `npm run build` fails outright. The stub explains the whole
      // decision, including what to do if Web Push ever comes into scope.
      'firebase/messaging': fileURLToPath(
        new URL('./firebaseMessagingUnavailable.ts', import.meta.url),
      ),
    },
  },

  build: {
    // Vite 8's default, `baseline-widely-available`, with Chrome lowered from 111
    // to 101 - the oldest Android WebView measured on a device this application
    // claims to support. A Huawei MediaPad T3 on Android 7.0, exactly the
    // `minSdkVersion` of 24, runs Chrome 101 as its WebView and its Play Store
    // offers nothing newer (2026-09-19).
    //
    // What the old target cost on that tablet, seen rather than predicted:
    // `index.css` writes `min-height: 100vh` and then `100dvh`, saying the `vh`
    // "stays as the fallback for anything that does not know the unit". The
    // minifier removed it, because Chrome 111 knows `dvh` and the fallback looked
    // dead. Chrome 101 does not, so the shell lost its height and the footer
    // floated up the screen. A fallback written on purpose was deleted by the
    // one setting that decides who needs it.
    //
    // The other four entries are the default's, unchanged - Safari and iOS 16.4
    // are why the iOS deployment target is 16.4.
    //
    // **This lowers syntax and keeps CSS fallbacks; it polyfills nothing.** A
    // browser API newer than Chrome 101 still breaks there, exactly as
    // `AbortSignal.timeout` (Chrome 103) did in `api/refresh.ts`, and no tool in
    // this project checks for one. TRIGGER: a new web API used anywhere, or a
    // device whose WebView is older than 101.
    target: ['chrome101', 'edge111', 'firefox114', 'safari16.4', 'ios16.4'],
  },

  test: {
    // Unit tests live in `src`; repository guards live in `guards`, because
    // they read files and `src` is deliberately typed as browser-only.
    //
    // Naming both rather than letting Vitest sweep the project is what keeps it
    // off `e2e/`: its default pattern collects `*.spec.*` anywhere, which since
    // 14.1 means Playwright's journey, failed with "Playwright Test did not
    // expect test() to be called here" - two runners fighting over one file.
    //
    // Both suffixes under `src`, so a `.spec.ts` written there by habit still
    // runs. The failure mode being avoided is the quiet one: a test file that
    // matches no pattern does not fail, it simply never runs.
    include: [
      'src/**/*.{test,spec}.{ts,tsx}',
      'guards/**/*.test.ts',
    ],

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