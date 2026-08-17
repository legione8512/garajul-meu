import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import type { DocumentDetails } from '../api/endpoints/documents.ts'
import { ro } from '../i18n/locales/ro.ts'
import { paths } from '../routes/paths.ts'
import { renderApp } from '../test/renderApp.tsx'

const PROFILE = {
  id: '1', fullName: 'Marius Robert', email: 'marius@example.com',
  preferredLanguage: 'ro', timezone: 'Europe/Bucharest', emailVerified: true,
}

const STORED: DocumentDetails = {
  id: 'doc-1',
  type: 'RCA',
  validFrom: '2026-01-01',
  validUntil: '2027-01-01',
  provider: 'Allianz',
  referenceNumber: 'POL-12345',
  notes: 'pe hârtie',
  status: 'ACTIVE',
  daysRemaining: 137,
}

/** What the renewal creates: a new identifier and a period of its own. */
const RENEWED: DocumentDetails = {
  ...STORED,
  id: 'doc-2',
  validFrom: null,
  validUntil: '2028-01-01',
  provider: null,
  referenceNumber: null,
  notes: null,
}

interface Sent {
  patches: number
  renewals: number
  body: unknown
}

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function stubDocument(read: () => Response, write?: () => Response): Sent {
  const sent: Sent = { patches: 0, renewals: 0, body: null }

  vi.stubGlobal('fetch', vi.fn((input: string, init?: RequestInit) => {
    if (input.includes('/auth/refresh')) {
      return Promise.resolve(jsonResponse(200, {
        accessToken: 'fresh', expiresInSeconds: 600, refreshToken: null,
      }))
    }
    // Before the document branches, deliberately: the renew path ends in the
    // document's own id, so checking that one first would send every renewal to
    // the read stub and the test would pass for the wrong reason.
    if (input.includes('/renew')) {
      sent.renewals += 1
      sent.body = JSON.parse(init?.body as string)
      return Promise.resolve(write === undefined ? jsonResponse(201, RENEWED) : write())
    }
    // The record a renewal created answers as itself, so "did it move to the new
    // one" is a question this stub can actually be wrong about.
    if (input.includes('doc-2')) {
      return Promise.resolve(jsonResponse(200, RENEWED))
    }
    if (input.includes('/documents/')) {
      if (init?.method === 'PATCH') {
        sent.patches += 1
        sent.body = JSON.parse(init.body as string)
        return Promise.resolve(write === undefined
          ? jsonResponse(200, { ...STORED, notes: null })
          : write())
      }
      return Promise.resolve(read())
    }
    return Promise.resolve(jsonResponse(200, PROFILE))
  }))

  return sent
}

/**
 * The five period labels appear twice on this screen, once per form. Correcting
 * is first in the document and renewing second, which is what these read - the
 * alternative was different wording for the same field in two places, and that
 * would have undone the reuse 10.6b exists to introduce.
 */
const correctionField = (label: string) => screen.getAllByLabelText(label)[0]
const renewalField = (label: string) => screen.getAllByLabelText(label)[1]

async function open() {
  renderApp(paths.document('a', 'doc-1'))
  await screen.findByRole('heading', { name: ro.documents.type.RCA, level: 1 })
}

describe('document details', () => {
  it('shows the stored document and its state', async () => {
    stubDocument(() => jsonResponse(200, STORED))

    await open()

    expect(screen.getByText(
      ro.documents.state.active.replace('{{days}}', '137'),
    )).toBeInTheDocument()
    expect(correctionField(ro.documents.fields.provider)).toHaveValue('Allianz')
    expect(correctionField(ro.documents.fields.validUntil)).toHaveValue('2027-01-01')
  })

  /**
   * The property that makes screen 13 worth having. A correction replaces the
   * record, so a note the person emptied has to leave as null - anything else
   * and there is no way to remove one at all.
   */
  it('a correction sends the whole record, and an emptied field as null', async () => {
    const sent = stubDocument(() => jsonResponse(200, STORED))

    await open()
    await userEvent.clear(correctionField(ro.documents.fields.notes))
    await userEvent.click(screen.getByRole('button', { name: ro.documents.saveCorrection }))

    await screen.findByRole('button', { name: ro.documents.saveCorrection })

    expect(sent.patches).toBe(1)
    expect(sent.body).toEqual({
      type: 'RCA',
      validFrom: '2026-01-01',
      validUntil: '2027-01-01',
      provider: 'Allianz',
      referenceNumber: 'POL-12345',
      notes: null,
    })
  })

  /** The renewal takes nothing from the record it supersedes but the type, which it does not send. */
  it('a renewal sends only a period, with no type at all', async () => {
    const sent = stubDocument(() => jsonResponse(200, STORED))

    await open()
    await userEvent.type(renewalField(ro.documents.fields.validUntil), '2028-01-01')
    await userEvent.click(screen.getByRole('button', { name: ro.documents.saveRenewal }))

    await screen.findByText(
      ro.documents.period.replace('{{until}}', new Date('2028-01-01').toLocaleDateString('ro')),
    )

    expect(sent.renewals).toBe(1)
    expect(sent.body).toEqual({
      validFrom: null,
      validUntil: '2028-01-01',
      provider: null,
      referenceNumber: null,
      notes: null,
    })
  })

  /**
   * Staying would show the superseded record as though it were current, which is
   * the mistake section 11 spends a paragraph forbidding on the dashboard.
   *
   * <p>The second assertion is the one that found a real defect: React Router
   * keeps a component mounted when only a path parameter changes, so without the
   * key the renewal form arrives at the new document still holding what created
   * it.
   */
  it('renewing opens the record it created, with its forms fresh', async () => {
    stubDocument(() => jsonResponse(200, STORED))

    await open()
    await userEvent.type(renewalField(ro.documents.fields.validUntil), '2028-01-01')
    await userEvent.click(screen.getByRole('button', { name: ro.documents.saveRenewal }))

    expect(await screen.findByText(
      ro.documents.period.replace('{{until}}', new Date('2028-01-01').toLocaleDateString('ro')),
    )).toBeInTheDocument()

    expect(renewalField(ro.documents.fields.validUntil)).toHaveValue('')
    expect(correctionField(ro.documents.fields.provider)).toHaveValue('')
  })

  it('the renewal form refuses an empty expiry without asking the backend', async () => {
    const sent = stubDocument(() => jsonResponse(200, STORED))

    await open()
    await userEvent.click(screen.getByRole('button', { name: ro.documents.saveRenewal }))

    expect(sent.renewals).toBe(0)
    expect(renewalField(ro.documents.fields.validUntil))
      .toHaveAccessibleDescription(ro.validation.required)
  })

  it('a refusal from the backend is translated and changes nothing', async () => {
    const sent = stubDocument(
      () => jsonResponse(200, STORED),
      () => jsonResponse(400, { code: 'DOCUMENT_INVALID_DATE_RANGE' }),
    )

    await open()
    await userEvent.click(screen.getByRole('button', { name: ro.documents.saveCorrection }))

    expect(await screen.findByText(ro.errors.DOCUMENT_INVALID_DATE_RANGE)).toBeInTheDocument()
    expect(sent.patches).toBe(1)
    expect(correctionField(ro.documents.fields.provider)).toHaveValue('Allianz')
  })

  it('a document that is not there says so', async () => {
    stubDocument(() => jsonResponse(404, { code: 'DOCUMENT_NOT_FOUND' }))

    renderApp(paths.document('a', 'doc-1'))

    expect(await screen.findByRole('alert'))
      .toHaveTextContent(ro.errors.DOCUMENT_NOT_FOUND)
    expect(screen.queryByRole('button', { name: ro.documents.saveCorrection }))
      .not.toBeInTheDocument()
  })
})