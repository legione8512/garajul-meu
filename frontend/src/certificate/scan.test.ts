import { describe, expect, it } from 'vitest'

import type { OcrScan, ProposedField } from '../api/endpoints/ocr.ts'
import { applyScan, tally } from './scan.ts'
import { fromForm, toForm, type CertificateForm } from './values.ts'
import type { CertificateData } from '../api/endpoints/certificate.ts'

const empty = {} as CertificateData

function blank(): CertificateForm {
  return toForm(empty)
}

function scanOf(...fields: ProposedField[]): OcrScan {
  return { fields }
}

const detected = (field: string, value: string): ProposedField =>
  ({ field, value, confidence: 0.95, status: 'DETECTED' })

const review = (field: string): ProposedField =>
  ({ field, value: null, confidence: 0.4, status: 'NEEDS_REVIEW' })

const missing = (field: string): ProposedField =>
  ({ field, value: null, confidence: 0, status: 'NOT_DETECTED' })

describe('applying a scan to the certificate form', () => {
  it('writes a detected value into the form', () => {
    const { form } = applyScan(blank(), scanOf(detected('make', 'Dacia')))

    expect(form.make).toBe('Dacia')
  })

  /**
   * The property that protects work already done. Somebody types their VIN,
   * photographs a creased certificate, and the scan cannot read it - the VIN
   * must still be there afterwards.
   */
  it('leaves a field alone when nothing was detected for it', () => {
    const typed = { ...blank(), vin: 'VF1AAAAAAAA000001' }

    const { form } = applyScan(typed, scanOf(missing('vin')))

    expect(form.vin).toBe('VF1AAAAAAAA000001')
  })

  it('leaves a field alone when the reading needs review and carries no value', () => {
    const typed = { ...blank(), registrationNumber: 'B 100 ABC' }

    const { form, statuses } = applyScan(typed, scanOf(review('registrationNumber')))

    expect(form.registrationNumber).toBe('B 100 ABC')
    expect(statuses.registrationNumber).toBe('NEEDS_REVIEW')
  })

  it('records a status for every field the scan mentions', () => {
    const { statuses } = applyScan(blank(), scanOf(
      detected('make', 'Dacia'),
      review('vin'),
      missing('colour'),
    ))

    expect(statuses).toEqual({ make: 'DETECTED', vin: 'NEEDS_REVIEW', colour: 'NOT_DETECTED' })
  })

  it('leaves fields the scan never mentions without a status', () => {
    const { statuses } = applyScan(blank(), scanOf(detected('make', 'Dacia')))

    expect(statuses.observations).toBeUndefined()
  })

  /**
   * A backend one version ahead must not be able to put a key into the form that
   * no input reads and no save would send.
   */
  it('ignores a field name this build does not know', () => {
    const { form, statuses } = applyScan(blank(), scanOf(detected('emissionsClass', 'Euro 6')))

    expect('emissionsClass' in form).toBe(false)
    expect('emissionsClass' in statuses).toBe(false)
  })

  /**
   * The contract between the two sides, asserted end to end rather than
   * described: the backend proposes what the field can hold, the form holds it
   * as typed, and fromForm turns it back into what the API accepts. A date and a
   * decimal are the two that would break if either side started converting.
   */
  it('produces values the save endpoint accepts, with no conversion in between', () => {
    const { form } = applyScan(blank(), scanOf(
      detected('firstRegistrationDate', '2019-03-14'),
      detected('maximumPowerKw', '66.5'),
      detected('seats', '5'),
    ))

    const data = fromForm(form)

    expect(data.firstRegistrationDate).toBe('2019-03-14')
    expect(data.maximumPowerKw).toBe(66.5)
    expect(data.seats).toBe(5)
  })

  it('counts the three states for the message the screen shows', () => {
    const { statuses } = applyScan(blank(), scanOf(
      detected('make', 'Dacia'),
      detected('colour', 'albastru'),
      review('vin'),
      missing('seats'),
    ))

    expect(tally(statuses)).toEqual({ detected: 2, needsReview: 1, notDetected: 1 })
  })
})