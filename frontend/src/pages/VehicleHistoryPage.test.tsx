import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import type { DocumentDetails } from '../api/endpoints/documents.ts'
import type { PageResponse } from '../api/page.ts'
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
  validFrom: null,
  validUntil: '2027-01-01',
  provider: null,
  referenceNumber: null,
  notes: null,
  status: 'ACTIVE',
  daysRemaining: 137,
}

const ITP: DocumentDetails = { ...RCA, id: 'doc-2', type: 'ITP', validUntil: '2026-11-01' }

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function page(items: DocumentDetails[], overrides: Partial<PageResponse<DocumentDetails>> = {}) {
  return {
    items,
    page: 0,
    size: 20,
    totalElements: items.length,
    totalPages: 1,
    ...overrides,
  }
}

/** Records every address asked for, because the filter and the page live in it. */
function stubHistory(answer: (url: string) => Response): string[] {
  const asked: string[] = []

  vi.stubGlobal('fetch', vi.fn((input: string) => {
    if (input.includes('/auth/refresh')) {
      return Promise.resolve(jsonResponse(200, {
        accessToken: 'fresh', expiresInSeconds: 600, refreshToken: null,
      }))
    }
    if (input.includes('/history')) {
      asked.push(input)
      return Promise.resolve(answer(input))
    }
    return Promise.resolve(jsonResponse(200, PROFILE))
  }))

  return asked
}

describe('document history', () => {
  it('says plainly when there is nothing yet', async () => {
    stubHistory(() => jsonResponse(200, page([])))

    renderApp(paths.history('a'))

    expect(await screen.findByText(ro.history.none)).toBeInTheDocument()
  })

  it('lists what was kept, newest entry first', async () => {
    stubHistory(() => jsonResponse(200, page([RCA, ITP])))

    renderApp(paths.history('a'))

    const entries = await screen.findAllByRole('heading', { level: 2 })
    expect(entries.map(entry => entry.textContent))
      .toEqual([ro.documents.type.RCA, ro.documents.type.ITP])
  })

  /** The filter is part of the address, which is what makes the reload free. */
  it('narrowing to one type asks the server for that type', async () => {
    const asked = stubHistory(() => jsonResponse(200, page([ITP])))

    renderApp(paths.history('a'))
    await screen.findByRole('heading', { level: 2 })

    await userEvent.selectOptions(
      screen.getByLabelText(ro.history.filter), 'ITP',
    )

    expect(await screen.findByRole('heading', { name: ro.documents.type.ITP })).toBeInTheDocument()
    expect(asked.at(-1)).toContain('type=ITP')
  })

  it('turning the page asks for the next one and says where you are', async () => {
    const asked = stubHistory(url => jsonResponse(200,
      url.includes('page=1')
        ? page([ITP], { page: 1, size: 1, totalElements: 2, totalPages: 2 })
        : page([RCA], { page: 0, size: 1, totalElements: 2, totalPages: 2 })))

    renderApp(paths.history('a'))

    expect(await screen.findByText(
      ro.history.page.replace('{{page}}', '1').replace('{{pages}}', '2'),
    )).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: ro.history.next }))

    expect(await screen.findByText(
      ro.history.page.replace('{{page}}', '2').replace('{{pages}}', '2'),
    )).toBeInTheDocument()
    expect(asked.at(-1)).toContain('page=1')
  })

  it('the ends of the list are not clickable', async () => {
    stubHistory(() => jsonResponse(200, page([RCA])))

    renderApp(paths.history('a'))
    await screen.findByRole('heading', { level: 2 })

    expect(screen.getByRole('button', { name: ro.history.previous })).toBeDisabled()
    expect(screen.getByRole('button', { name: ro.history.next })).toBeDisabled()
  })

  /**
   * Page three of one filter is very likely past the end of another, and an
   * empty screen would read as "no history" rather than "no such page".
   */
  it('changing the filter goes back to the first page', async () => {
    const asked = stubHistory(() => jsonResponse(200,
      page([RCA], { page: 0, size: 1, totalElements: 2, totalPages: 2 })))

    renderApp(paths.history('a'))
    await screen.findByRole('heading', { level: 2 })

    await userEvent.click(screen.getByRole('button', { name: ro.history.next }))
    await screen.findByRole('heading', { level: 2 })

    await userEvent.selectOptions(screen.getByLabelText(ro.history.filter), 'RCA')

    await screen.findByRole('heading', { level: 2 })
    expect(asked.at(-1)).toContain('type=RCA')
    expect(asked.at(-1)).not.toContain('page=')
  })
})