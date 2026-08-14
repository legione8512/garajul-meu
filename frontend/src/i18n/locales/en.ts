import type { ro } from './ro.ts'

/**
 * Typed as the Romanian object, so this file cannot drift: a missing key and an
 * invented one are both compile errors.
 */
export const en: typeof ro = {
  app: {
    name: 'Garajul Meu',
  },
  common: {
    reference: 'Reference: {{requestId}}',
  },
  language: {
    label: 'Language',
  },
  screens: {
    welcome: 'Welcome',
    register: 'Create account',
    verifyEmail: 'Email verification',
    login: 'Sign in',
    forgotPassword: 'Forgot your password',
    resetPassword: 'New password',
    notFound: 'Page not found',
  },
  validation: {
    required: 'Please fill in this field.',
    email: 'That does not look like an email address.',
    minLength: 'Minimum length: {{min}} characters.',
    maxLength: 'Maximum length: {{max}} characters.',
    sixDigits: 'The code is exactly six digits.',
    invalid: 'That value is not accepted.',
  },
  errors: {
    AUTHENTICATION_REQUIRED: 'Your session has expired. Please sign in again.',
    INVALID_CREDENTIALS: 'That email address or password is not correct.',
    EMAIL_NOT_VERIFIED: 'This email address is not confirmed yet. Check your inbox.',
    REFRESH_TOKEN_INVALID: 'This session is no longer valid. Please sign in again.',
    REFRESH_TOKEN_REUSED: 'The session was closed for security reasons. Please sign in again.',
    VERIFICATION_CODE_INVALID: 'That code is not correct.',
    VERIFICATION_CODE_EXPIRED: 'That code has expired. Request a new one.',
    EMAIL_ALREADY_EXISTS: 'An account with this email address already exists.',
    USER_NOT_FOUND: 'Account not found.',
    INVALID_CURRENT_PASSWORD: 'Your current password is not correct.',
    VALIDATION_ERROR: 'Please check the details you entered.',
    MALFORMED_REQUEST: 'The request could not be read. Please try again.',
    RESOURCE_NOT_FOUND: 'Not found.',
    METHOD_NOT_ALLOWED: 'That operation is not allowed here.',
    RATE_LIMITED: 'Too many attempts. Please wait a moment and try again.',
    INTERNAL_ERROR: 'Something went wrong. Please try again later.',
    UNKNOWN: 'Something went wrong. Please try again later.',
  },
}