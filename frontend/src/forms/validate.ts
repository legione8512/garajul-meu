import type { ApiError } from '../api/ApiError.ts'
import { GENERIC_MESSAGE, type Rule, type ValidationMessage } from './rules.ts'

export type FieldRules<F extends string> = Partial<Record<F, readonly Rule[]>>

export type FieldMessages<F extends string> = Partial<Record<F, ValidationMessage>>

function firstBroken(value: string, rules: readonly Rule[]): Rule | undefined {
  return rules.find((rule) => !rule.test(value))
}

/**
 * Checks every field before anything is sent. Only the first broken rule per
 * field is reported: three complaints about one input is noise, and the person
 * can only fix one thing at a time anyway.
 */
export function validate<F extends string>(
  values: Readonly<Record<F, string>>,
  rules: FieldRules<F>,
): FieldMessages<F> {
  const messages: FieldMessages<F> = {}

  for (const field of Object.keys(values) as F[]) {
    const broken = firstBroken(values[field], rules[field] ?? [])

    if (broken !== undefined) {
      messages[field] = broken.message
    }
  }

  return messages
}

/**
 * Turns the backend's field errors into the same messages this form would have
 * produced itself.
 *
 * <p>The backend sends {@code {field, constraint}} and no parameters, so a Size
 * violation arrives without its bound. Rather than answer with something vague,
 * the form looks up its own rule for that field and reuses its wording - which
 * it can, because it had to know the bound to validate locally in the first
 * place.
 */
export function fieldMessagesFrom<F extends string>(
  error: ApiError,
  values: Readonly<Record<F, string>>,
  rules: FieldRules<F>,
): FieldMessages<F> {
  const messages: FieldMessages<F> = {}

  for (const fieldError of error.fieldErrors) {
    const field = fieldError.field as F
    const candidates = (rules[field] ?? []).filter((rule) => rule.constraint === fieldError.constraint)

    // Prefer whichever candidate actually fails for the submitted value:
    // minLength and maxLength are both reported as Size, and only one of them
    // can be the reason.
    const broken = candidates.find((rule) => !rule.test(values[field] ?? ''))

    messages[field] = (broken ?? candidates[0])?.message ?? GENERIC_MESSAGE
  }

  return messages
}