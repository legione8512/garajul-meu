import { describe, expect, it } from 'vitest'

import type { CertificateData } from '../api/endpoints/certificate.ts'
import { certificateFields } from './fields.ts'
import { fromForm, toForm } from './values.ts'

const FULL: CertificateData = {
  registrationNumber: 'B 100 ABC',
  firstRegistrationDate: '2019-03-14',
  vehicleCategory: 'M1',
  make: 'Dacia',
  typeVariantVersion: 'SD/JSD/AB1',
  commercialDescription: 'Logan',
  vin: 'VF1AAAAAAAA000001',
  typeApprovalNumber: 'e2*2007/46*0123*05',
  validityPeriod: 'nelimitat',
  registrationDate: '2021-07-01',
  certificateIssueDate: '2021-07-05',
  maximumPermissibleMassKg: 1780,
  vehicleMassKg: 1165,
  engineCapacityCc: 999,
  maximumPowerKw: 66,
  fuelType: 'benzina',
  powerWeightRatio: 56.652,
  colour: 'albastru',
  seats: 5,
  standingPlaces: 0,
  civNumber: 'K123456',
  issuingAuthority: 'DRPCIV',
  observations: 'Fara observatii',
  certificateNumber: 'A00123456',
  ownerNameOrCompany: 'Robert',
  ownerFirstName: 'Marius',
  ownerAddress: 'Str. Exemplu 1',
  c2EqualsC1: true,
  userNameOrCompany: null,
  userFirstName: null,
  userAddress: null,
  c3EqualsC1: false,
}

describe('certificate values', () => {
  /**
   * The guard against a field that exists in the type and not in the table.
   * Nothing else would notice: the screen would simply never render it, and the
   * save would clear it every time.
   */
  it('the field table covers every field the API sends', () => {
    expect(certificateFields).toHaveLength(Object.keys(FULL).length)
    expect(certificateFields.map(field => field.name).sort())
      .toEqual(Object.keys(FULL).sort())
  })

  it('a full certificate survives a round trip', () => {
    expect(fromForm(toForm(FULL))).toEqual(FULL)
  })

  it('absent values become empty strings and come back as nulls', () => {
    const form = toForm({ ...FULL, colour: null, seats: null, observations: null })

    expect(form.colour).toBe('')
    expect(form.seats).toBe('')

    const sent = fromForm(form)
    expect(sent.colour).toBeNull()
    expect(sent.seats).toBeNull()
  })

  /** Digits in quotes would fail the backend's own typing, not its validation. */
  it('numbers are sent as numbers', () => {
    const sent = fromForm({ ...toForm(FULL), seats: '5', maximumPowerKw: '66.5' })

    expect(sent.seats).toBe(5)
    expect(sent.maximumPowerKw).toBe(66.5)
  })

  /** Nonsense becomes absent rather than NaN, which no arithmetic survives. */
  it('a number that is not one becomes nothing', () => {
    expect(fromForm({ ...toForm(FULL), engineCapacityCc: 'o mie' }).engineCapacityCc).toBeNull()
  })

  /**
   * The certificate's box is ticked or it is not. Unknown and unticked are the
   * same thing to anyone reading the document, so an untouched box is sent as
   * false rather than preserved as null.
   */
  it('checkboxes are booleans, and an untouched one is false', () => {
    const form = toForm({ ...FULL, c2EqualsC1: true, c3EqualsC1: null })

    expect(form.c2EqualsC1).toBe('true')
    expect(form.c3EqualsC1).toBe('')

    const sent = fromForm(form)
    expect(sent.c2EqualsC1).toBe(true)
    expect(sent.c3EqualsC1).toBe(false)
  })
    /**
   * Romanian writes 66,5. Number() answers NaN for that, which this code turns
   * into null - so without the comma, somebody entering the power of their own
   * car in their own language would watch the field empty itself on save.
   */
  it('reads a decimal written with a comma', () => {
    const sent = fromForm({ ...toForm(FULL), maximumPowerKw: '66,5', powerWeightRatio: '56,652' })

    expect(sent.maximumPowerKw).toBe(66.5)
    expect(sent.powerWeightRatio).toBe(56.652)
  })

  /** A whole number has no decimal separator, so any separator groups digits. */
  it('reads a whole number however it is grouped', () => {
    expect(fromForm({ ...toForm(FULL), maximumPermissibleMassKg: '1.780' }).maximumPermissibleMassKg).toBe(1780)
    expect(fromForm({ ...toForm(FULL), vehicleMassKg: '1 165' }).vehicleMassKg).toBe(1165)
  })
})