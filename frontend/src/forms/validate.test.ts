import { describe, expect, it } from 'vitest'

import { ApiError } from '../api/ApiError.ts'
import { emailShape, maxLength, minLength, required, sixDigitCode } from './rules.ts'
import { fieldMessagesFrom, validate } from './validate.ts'

const rules = {
  email: [required, emailShape, maxLength(320)],
  password: [required, minLength(12), maxLength(128)],
  code: [required, sixDigitCode],
}

describe('client validation', () => {
  it('accepts values that satisfy every rule', () => {
    const values = { email: 'marius@example.com', password: 'a-long-enough-password', code: '123456' }

    expect(validate(values, rules)).toEqual({})
  })

  it('reports the first rule each value breaks and no more', () => {
    const messages = validate({ email: '', password: 'short', code: '12' }, rules)

    expect(messages.email?.key).toBe('validation.required')
    expect(messages.password?.key).toBe('validation.minLength')
    expect(messages.code?.key).toBe('validation.sixDigits')
  })
})

describe('server field errors', () => {
  it("answers with the form's own wording rather than something vaguer", () => {
    const error = new ApiError('VALIDATION_ERROR', 400, null, [{ field: 'password', constraint: 'Size' }])

    const messages = fieldMessagesFrom(error, { email: '', password: 'short', code: '' }, rules)

    expect(messages.password).toEqual({ key: 'validation.minLength', values: { min: 12 } })
  })

  /** Size covers both bounds, and only one of them can be the reason. */
  it('picks the bound the submitted value actually breaks', () => {
    const error = new ApiError('VALIDATION_ERROR', 400, null, [{ field: 'password', constraint: 'Size' }])

    const messages = fieldMessagesFrom(error, { email: '', password: 'x'.repeat(200), code: '' }, rules)

    expect(messages.password).toEqual({ key: 'validation.maxLength', values: { max: 128 } })
  })

  it('falls back to a generic message for a constraint it has no rule for', () => {
    const error = new ApiError('VALIDATION_ERROR', 400, null, [{ field: 'email', constraint: 'AssertTrue' }])

    const messages = fieldMessagesFrom(error, { email: 'marius@example.com', password: '', code: '' }, rules)

    expect(messages.email?.key).toBe('validation.invalid')
  })
})