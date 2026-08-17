import { screen, waitFor } from '@testing-library/react'
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

const RCA: DocumentDetails = {
  id: 'doc-1',
  type: 'RCA',
  validFrom: '2026-01-01',
  validUntil: '2027-01-01',
  provider: 'Allianz',
  referenceNumber: 'POL-12345',
  notes: null,
  status: 'ACTIVE',
  daysRemaining: 137,
}

interface Sent {
  posts: number
  deletes: number
  body: unknown
  lists: number
}

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function stubDocuments(pages: (() => Response)[], write?: () => Response): Sent {
  const sent: Sent = { posts: 0, deletes: 0, body: null, lists: 0 }

  vi.stubGlobal('fetch', vi.fn((input: string, init?: RequestInit) => {
    if (input.includes('/auth/refresh')) {
      return Promise.resolve(jsonResponse(200, {
        accessToken: 'fresh', expiresInSeconds: 600, refreshToken: null,
      }))
    }
    if (input.includes('/documents')) {
      if (init?.method === 'POST') {
        sent.posts += 1
        sent.body = JSON.parse(init.body as string)
        return Promise.resolve(write === undefined ? jsonResponse(201, RCA) : write())
      }
      if (init?.method === 'DELETE') {
        sent.deletes += 1
        return Promise.resolve(new Response(null, { status: 204 }))
      }
      // Each GET answers with the next canned list, so a reload can differ.
      const answer = pages[Math.min(sent.lists, pages.length - 1)]
      sent.lists += 1
      return Promise.resolve(answer())
    }
    return Promise.resolve(jsonResponse(200, PROFILE))
  }))

  return sent
}

async function open() {
  renderApp(paths.documents('a'))
  await screen.findByRole('heading', { name: ro.documents.title, level: 1 })
}

describe('vehicle documents', () => {
  it('says plainly when a vehicle has none', async () => {
    stubDocuments([() => jsonResponse(200, [])])

    await open()

    expect(await screen.findByText(ro.documents.none)).toBeInTheDocument()
  })

  it('shows a stored document with its period and its state', async () => {
    stubDocuments([() => jsonResponse(200, [RCA])])

    await open()

    expect(await screen.findByRole('heading', { name: ro.documents.type.RCA })).toBeInTheDocument()
    expect(screen.getByText('Allianz')).toBeInTheDocument()
    expect(screen.getByText('POL-12345')).toBeInTheDocument()
    expect(screen.getByText(
      ro.documents.state.active.replace('{{days}}', '137'),
    )).toBeInTheDocument()
  })

  /** The four section 8 requires of a period: only the end date is mandatory. */
  it('refuses an empty expiry date without asking the backend', async () => {
    const sent = stubDocuments([() => jsonResponse(200, [])])

    await open()
    await userEvent.click(screen.getByRole('button', { name: ro.documents.save }))

    expect(screen.getByLabelText(ro.documents.fields.validUntil))
      .toHaveAccessibleDescription(ro.validation.required)
    expect(sent.posts).toBe(0)
  })

  /**
   * An input left empty is "not given", which the API expects as null. Sending
   * "" would store an empty string where the column means absence.
   */
  it('sends untouched optional fields as null rather than empty strings', async () => {
    const sent = stubDocuments([() => jsonResponse(200, [])])

    await open()
    await userEvent.type(screen.getByLabelText(ro.documents.fields.validUntil), '2027-01-01')
    await userEvent.click(screen.getByRole('button', { name: ro.documents.save }))

    // The list stays empty by design here, so what a success looks like on screen
    // is the form clearing itself - which is also worth asserting.
    await waitFor(() => {
      expect(screen.getByLabelText(ro.documents.fields.validUntil)).toHaveValue('')
    })
    
    expect(sent.posts).toBe(1)
    expect(sent.body).toEqual({
      type: 'RCA',
      validFrom: null,
      validUntil: '2027-01-01',
      provider: null,
      referenceNumber: null,
      notes: null,
    })
  })

  it('reloads the list after adding one', async () => {
    const sent = stubDocuments([
      () => jsonResponse(200, []),
      () => jsonResponse(200, [RCA]),
    ])

    await open()
    await userEvent.type(screen.getByLabelText(ro.documents.fields.validUntil), '2027-01-01')
    await userEvent.click(screen.getByRole('button', { name: ro.documents.save }))

    expect(await screen.findByRole('heading', { name: ro.documents.type.RCA })).toBeInTheDocument()
    expect(sent.lists).toBe(2)
  })

  /** Deleting asks first, in the application's own language. */
  it('confirms before deleting and reloads afterwards', async () => {
    const sent = stubDocuments([
      () => jsonResponse(200, [RCA]),
      () => jsonResponse(200, []),
    ])

    await open()
    await userEvent.click(await screen.findByRole('button', { name: ro.documents.delete }))

    expect(screen.getByText(ro.documents.confirmDelete)).toBeInTheDocument()
    expect(sent.deletes).toBe(0)

    await userEvent.click(screen.getByRole('button', { name: ro.documents.confirmDeleteYes }))

    expect(await screen.findByText(ro.documents.none)).toBeInTheDocument()
    expect(sent.deletes).toBe(1)
  })

  it('changing your mind deletes nothing', async () => {
    const sent = stubDocuments([() => jsonResponse(200, [RCA])])

    await open()
    await userEvent.click(await screen.findByRole('button', { name: ro.documents.delete }))
    await userEvent.click(screen.getByRole('button', { name: ro.documents.cancel }))

    expect(screen.queryByText(ro.documents.confirmDelete)).not.toBeInTheDocument()
    expect(sent.deletes).toBe(0)
  })

  it('a backend refusal is translated and nothing is added', async () => {
    const sent = stubDocuments(
      [() => jsonResponse(200, [])],
      () => jsonResponse(400, { code: 'DOCUMENT_INVALID_DATE_RANGE' }),
    )

    await open()
    await userEvent.type(screen.getByLabelText(ro.documents.fields.validUntil), '2027-01-01')
    await userEvent.click(screen.getByRole('button', { name: ro.documents.save }))

    expect(await screen.findByText(ro.errors.DOCUMENT_INVALID_DATE_RANGE)).toBeInTheDocument()
    expect(sent.posts).toBe(1)
    expect(sent.lists).toBe(1)
  })
})