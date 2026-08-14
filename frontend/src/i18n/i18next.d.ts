import type { ro } from './locales/ro.ts'

/**
 * Makes t() check its argument. A mistyped key stops the build instead of
 * rendering the key itself to the reader - the frontend counterpart of the
 * backend's ErrorCode enum, where an invented code cannot compile either.
 */
declare module 'i18next' {
  interface CustomTypeOptions {
    defaultNS: 'translation'
    resources: {
      translation: typeof ro
    }
  }
}