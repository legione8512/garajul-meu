import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import tseslint from 'typescript-eslint'
import { defineConfig, globalIgnores } from 'eslint/config'

export default defineConfig([
  /*
   * The three places a build writes, and `dist` was the only one when this line
   * was written. The native projects arrived with phase 17 and 18, and each
   * keeps its own copy of Capacitor's `native-bridge.js` under a build
   * directory - JavaScript this project did not write, cannot fix, and would be
   * reporting on for ever.
   *
   * It surfaced as three lint problems in
   * `android/app/build/intermediates/assets/debug/...`, none of them about
   * anything under `src`. A gate that reports on generated code is a gate people
   * learn to skim.
   *
   * Only the build outputs, deliberately - not `android/` and `ios/` whole. Those
   * are committed and hand-edited, and if a real script is ever added to one it
   * should be linted like anything else.
   */
  globalIgnores(['dist', 'android/**/build/**', 'ios/**/build/**', 'ios/**/DerivedData/**']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      tseslint.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
    ],
    languageOptions: {
      globals: globals.browser,
    },
  },
])
