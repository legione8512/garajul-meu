import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import type { CertificateData } from '../api/endpoints/certificate.ts'
import { certificateFields } from '../certificate/fields.ts'
import { ro } from '../i18n/locales/ro.ts'
import { paths } from '../routes/paths.ts'
import { renderApp } from '../test/renderApp.tsx'

const PROFILE = {
  id: '1', fullName: 'Marius Robert', email: 'marius@example.com',
  preferredLanguage: 'ro', timezone: 'Europe/Bucharest', emailVerified: true,
}

const STORED: CertificateData = {
  registrationNumber: 'B 100 ABC',
  firstRegistrationDate: '2019-03-14',
  vehicleCategory: 'M1',
  make: 'Dacia',
  typeVariantVersion: null,
  commercialDescription: 'Logan',
  vin: 'VF1AAAAAAAA000001',
  typeApprovalNumber: null,
  validityPeriod: null,
  registrationDate: null,
  certificateIssueDate: null,
  maximumPermissibleMassKg: 1780,
  vehicleMassKg: null,
  engineCapacityCc: 999,
  maximumPowerKw: 66,
  fuelType: 'benzina',
  powerWeightRatio: null,
  colour: 'albastru',
  seats: 5,
  standingPlaces: null,
  civNumber: null,
  issuingAuthority: 'DRPCIV',
  observations: null,
  certificateNumber: 'A00123456',
  ownerNameOrCompany: 'Robert',
  ownerFirstName: 'Marius',
  ownerAddress: 'Str. Exemplu 1',
  c2EqualsC1: true,
  userNameOrCompany: null,
  userFirstName: null,
  userAddress: null,
  c3EqualsC1: null,
}

interface Sent {
  body: CertificateData | null
  patches: number
}

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function stubCertificate(read: () => Response, write?: () => Response): Sent {
  const sent: Sent = { body: null, patches: 0 }

  vi.stubGlobal('fetch', vi.fn((input: string, init?: RequestInit) => {
    if (input.includes('/auth/refresh')) {
      return Promise.resolve(jsonResponse(200, {
        accessToken: 'fresh', expiresInSeconds: 600, refreshToken: null,
      }))
    }
    if (input.includes('/registration-certificate')) {
      if (init?.method === 'PATCH') {
        sent.patches += 1
        sent.body = JSON.parse(init.body as string) as CertificateData
        return Promise.resolve(write === undefined ? jsonResponse(200, sent.body) : write())
      }
      return Promise.resolve(read())
    }
    return Promise.resolve(jsonResponse(200, PROFILE))
  }))

  return sent
}

async function open() {
  renderApp(paths.certificate('a'))
  await screen.findByLabelText(ro.certificate.fields.vin)
}

describe('registration certificate', () => {
  it('shows what is stored, grouped as the document is', async () => {
    stubCertificate(() => jsonResponse(200, STORED))

    await open()

    expect(screen.getByLabelText(ro.certificate.fields.vin)).toHaveValue('VF1AAAAAAAA000001')
    expect(screen.getByLabelText(ro.certificate.fields.engineCapacityCc)).toHaveValue('999')
    expect(screen.getByLabelText(ro.certificate.fields.firstRegistrationDate)).toHaveValue('2019-03-14')
    expect(screen.getByLabelText(ro.certificate.fields.c2EqualsC1)).toBeChecked()
    // Empty on the document is empty here, not the word "null".
    expect(screen.getByLabelText(ro.certificate.fields.vehicleMassKg)).toHaveValue('')

    const owner = screen.getByRole('group', { name: ro.certificate.groups.owner })
    expect(within(owner).getByText(ro.certificate.sensitiveNote)).toBeInTheDocument()
  })

  /**
   * Every field, every time. That is what makes the backend's replacement
   * semantics safe - a field this screen omitted would be cleared on the server.
   */
  it('saving sends the whole certificate, with numbers as numbers', async () => {
    const sent = stubCertificate(() => jsonResponse(200, STORED))

    await open()
    await userEvent.clear(screen.getByLabelText(ro.certificate.fields.colour))
    await userEvent.type(screen.getByLabelText(ro.certificate.fields.colour), 'rosu')
    await userEvent.click(screen.getByRole('button', { name: ro.certificate.save }))

    await screen.findByText(ro.certificate.saved)

    expect(sent.patches).toBe(1)
    expect(Object.keys(sent.body ?? {})).toHaveLength(certificateFields.length)
    expect(sent.body?.colour).toBe('rosu')
    expect(sent.body?.seats).toBe(5)
    expect(sent.body?.engineCapacityCc).toBe(999)
  })

  it('a decimal written with a comma survives the trip', async () => {
    const sent = stubCertificate(() => jsonResponse(200, STORED))

    await open()
    await userEvent.clear(screen.getByLabelText(ro.certificate.fields.maximumPowerKw))
    await userEvent.type(screen.getByLabelText(ro.certificate.fields.maximumPowerKw), '66,5')
    await userEvent.click(screen.getByRole('button', { name: ro.certificate.save }))

    await screen.findByText(ro.certificate.saved)

    expect(sent.body?.maximumPowerKw).toBe(66.5)
  })

  it('emptying an optional field clears it rather than sending an empty string', async () => {
    const sent = stubCertificate(() => jsonResponse(200, STORED))

    await open()
    await userEvent.clear(screen.getByLabelText(ro.certificate.fields.colour))
    await userEvent.click(screen.getByRole('button', { name: ro.certificate.save }))

    await screen.findByText(ro.certificate.saved)

    expect(sent.body?.colour).toBeNull()
  })

  /** The four section 8 requires, refused here before they cost a request. */
  it('a required field left blank is refused without asking the backend', async () => {
    const sent = stubCertificate(() => jsonResponse(200, STORED))

    await open()
    await userEvent.clear(screen.getByLabelText(ro.certificate.fields.vin))
    await userEvent.click(screen.getByRole('button', { name: ro.certificate.save }))

    expect(screen.getByLabelText(ro.certificate.fields.vin))
      .toHaveAccessibleDescription(ro.validation.required)
    expect(sent.patches).toBe(0)
  })

  /** The 404 that also answers for a certificate belonging to somebody else. */
  it('a certificate that is not there says so', async () => {
    stubCertificate(() => jsonResponse(404, { code: 'VEHICLE_NOT_FOUND' }))

    renderApp(paths.certificate('a'))

    expect(await screen.findByRole('alert')).toHaveTextContent(ro.errors.VEHICLE_NOT_FOUND)
    expect(screen.queryByRole('button', { name: ro.certificate.save })).not.toBeInTheDocument()
  })
    /**
   * Found in a browser, not here: the link said "back to the garage" and led to
   * the vehicle. Asserting the name and the destination together is what makes a
   * label that lies about where it goes a failure rather than a nuisance.
   */
  it('the way back names the place it actually leads to', async () => {
    stubCertificate(() => jsonResponse(200, STORED))

    await open()

    expect(screen.getByRole('link', { name: ro.certificate.backToVehicle }))
      .toHaveAttribute('href', paths.vehicle('a'))
  })
})