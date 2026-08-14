import type { ro } from '../i18n/locales/ro.ts'

export type ValidationKey = `validation.${keyof typeof ro.validation}`

export interface ValidationMessage {
  readonly key: ValidationKey
  readonly values?: Readonly<Record<string, string | number>>
}

export interface Rule {
  /**
   * The Bean Validation annotation the backend reports for this same rule.
   * Spring sends the annotation's simple name - NotBlank, Size, Email, Pattern
   * - and matching it here is what lets a server-side field error reuse this
   * form's own wording instead of needing a second, vaguer set of messages.
   */
  readonly constraint: string
  readonly test: (value: string) => boolean
  readonly message: ValidationMessage
}

export const required: Rule = {
  constraint: 'NotBlank',
  test: (value) => value.trim().length > 0,
  message: { key: 'validation.required' },
}

export const emailShape: Rule = {
  constraint: 'Email',
  // Deliberately permissive. Strict address patterns reject perfectly valid
  // addresses, and the only real authority on whether one works is whether mail
  // sent to it arrives - which the verification code already establishes. This
  // catches typing mistakes and nothing more.
  test: (value) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value),
  message: { key: 'validation.email' },
}

export const sixDigitCode: Rule = {
  constraint: 'Pattern',
  test: (value) => /^\d{6}$/.test(value),
  message: { key: 'validation.sixDigits' },
}

export function minLength(min: number): Rule {
  return {
    constraint: 'Size',
    test: (value) => value.length >= min,
    message: { key: 'validation.minLength', values: { min } },
  }
}

export function maxLength(max: number): Rule {
  return {
    constraint: 'Size',
    test: (value) => value.length <= max,
    message: { key: 'validation.maxLength', values: { max } },
  }
}

/** What a constraint this form has no rule for falls back to. */
export const GENERIC_MESSAGE: ValidationMessage = { key: 'validation.invalid' }