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
   * The contract with the backend for everything the application can provoke
   * today. If one of these ever resolves to UNKNOWN, a real failure is being
   * shown to somebody as "something went wrong" while the backend said exactly
   * what happened.
   *
   * <p>Three codes in the enum are deliberately absent because nothing throws
   * them - checked rather than assumed: the two registration certificate codes
   * and VEHICLE_ACCESS_DENIED, which cannot be sent while a vehicle somebody
   * else owns answers 404.
   *
   * <p><strong>IMAGE_INVALID_TYPE and STORAGE_PROVIDER_UNAVAILABLE were on that
   * absent list until 2026-08-18, and this test is what noticed.</strong> Phase
   * 12.2 gave VehicleImageValidator the first and FileStorageProvider.put the
   * second, so the note claiming nothing threw them had quietly become false.
   * The list was written from what the backend could do on the day it was
   * written; the guard below is what stops it staying that way.
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
      'DOCUMENT_NOT_FOUND',
      'DOCUMENT_INVALID_DATE_RANGE',
      'DOCUMENT_TYPE_INVALID',
      'OCR_FILE_INVALID',
      'OCR_RATE_LIMITED',
      'OCR_PROVIDER_UNAVAILABLE',
      'OCR_PROCESSING_FAILED',
      'IMAGE_TOO_LARGE',
      'IMAGE_INVALID_TYPE',
      'STORAGE_PROVIDER_UNAVAILABLE',
      'VALIDATION_ERROR',
      'MALFORMED_REQUEST',
      'RESOURCE_NOT_FOUND',
      'METHOD_NOT_ALLOWED',
      'RATE_LIMITED',
      'INTERNAL_ERROR',
    ]

    for (const code of codes) {
      expect(errorMessageKey(code), code).not.toBe('errors.UNKNOWN')
      expect(ro.errors[code as keyof typeof ro.errors]).toBeTruthy()
    }

    // The other direction, and the reason this test exists twice over. Checking
    // only that listed codes have wording lets a translated code go unlisted,
    // and a code nobody lists is a code nobody is checking - which is how
    // Phase 9's five OCR codes sat outside this guard for two whole phases
    // while appearing, translated, on real screens.
    expect([...codes].sort()).toEqual(
      Object.keys(ro.errors).filter(code => code !== 'UNKNOWN').sort(),
    )
  })
})