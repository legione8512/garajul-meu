import { ro } from './locales/ro.ts'

export type ErrorMessageKey = `errors.${keyof typeof ro.errors}`

function isKnownErrorCode(code: string): code is keyof typeof ro.errors {
  return Object.hasOwn(ro.errors, code)
}

/**
 * Turns a backend error code into the key that translates it.
 *
 * <p>Deliberately returns a key rather than a finished sentence: the mapping is
 * the logic worth testing, and translating is i18next's job. It also keeps this
 * module free of React, so it is a plain function rather than a hook.
 *
 * <p>The unknown branch is the important one. The backend's catalogue will grow
 * in later phases, and a code this frontend has never seen must never reach a
 * reader as the literal text "errors.SOMETHING_NEW".
 */
export function errorMessageKey(code: string): ErrorMessageKey {
  return isKnownErrorCode(code) ? `errors.${code}` : 'errors.UNKNOWN'
}