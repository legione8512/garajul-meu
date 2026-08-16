import { describe, expect, it } from 'vitest'

import { errorMessageKey } from './errorKey.ts'
import { ro } from './locales/ro.ts'

describe('backend error codes', () => {
  it('maps a code it knows to its own key', () => {    expect(errorMessageKey('INVALID_CREDENTIALS')).toBe('errors.INVALID_CREDENTIALS')
  })

  it('maps a code it has never seen to the generic message', () => {
    expect(errorMessageKey('SOMETHING_A_LATER_PHASE_ADDS')).toBe('errors.UNKNOWN')
  })

  /**
   * The contract with the backend for everything Phase 5 can provoke. If one of
   * these ever resolves to UNKNOWN, a real failure is being shown to somebody
   * as "something went wrong" while the backend said exactly what happened.
   */
  it('has wording for every code the backend can currently send', () => {
    const codes = [
      'AUTHENTICATION_REQUIRED',
      'INVALID_CREDENTIALS',
      'EMAIL_NOT_VERIFIED',
      'REFRESH_TOKEN_INVALID',
      'REFRESH_TOKEN_REUSED',
      'VERIFICATION_CODE_INVALID',
      'VERIFICATION_CODE_EXPIRED',
      'EMAIL_ALREADY_EXISTS',
      'USER_NOT_FOUND',
      'INVALID_CURRENT_PASSWORD',
      'VEHICLE_NOT_FOUND',
      'VEHICLE_DUPLICATE_VIN',
      'VALIDATION_ERROR',
      'RATE_LIMITED',
      'INTERNAL_ERROR',
    ]

    for (const code of codes) {
      expect(errorMessageKey(code), code).not.toBe('errors.UNKNOWN')
      expect(ro.errors[code as keyof typeof ro.errors]).toBeTruthy()
    }
  })
})