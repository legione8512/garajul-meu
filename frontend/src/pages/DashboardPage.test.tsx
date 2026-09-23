import { screen, waitFor, within } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import type { DashboardView } from '../api/endpoints/dashboard.ts'
import { ro } from '../i18n/locales/ro.ts'
import { paths } from '../routes/paths.ts'
import { renderApp } from '../test/renderApp.tsx'

const PROFILE = {
  id: '1', fullName: 'Marius Robert', email: 'marius@example.com',
  preferredLanguage: 'ro', timezone: 'Europe/Bucharest', emailVerified: true,
}

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function stubDashboard(answer: () => Response) {
  vi.stubGlobal('fetch', vi.fn((input: string) => {
    if (input.includes('/auth/refresh')) {
      return Promise.resolve(jsonResponse(200, {
        accessToken: 'fresh', expiresInSeconds: 600, refreshToken: null,
      }))
    }
    // Before the dashboard branch: a thumbnail's address is a vehicle's, not the
    // dashboard's, but it is easy to add a branch in the wrong order.
    if (input.includes('/image/thumbnail')) {
      return Promise.resolve(new Response(new Blob(['bytes'], { type: 'image/jpeg' }), {
        status: 200,
        headers: { 'Content-Type': 'image/jpeg' },
      }))
    }
    if (input.includes('/dashboard')) {
      return Promise.resolve(answer())
    }
    return Promise.resolve(jsonResponse(200, PROFILE))
  }))
}

function garage(...documents: DashboardView['vehicles'][number]['documents']): DashboardView {
  return {
    vehicles: [{
      vehicleId: 'v1',
      displayName: null,
      registrationNumber: 'B 100 ABC',
      make: 'Dacia',
      commercialDescription: 'Logan',
      hasImage: false,
      documents,
    }],
  }
}

/** The same vehicle, photographed. */
function photographed(view: DashboardView): DashboardView {
  return { vehicles: view.vehicles.map(vehicle => ({ ...vehicle, hasImage: true })) }
}

describe('dashboard', () => {
  // jsdom has neither, and the card's picture is an object URL like any other.
  beforeEach(() => {
    URL.createObjectURL = vi.fn(() => 'blob:card-photograph')
    URL.revokeObjectURL = vi.fn()
  })

  it('offers a way in when the garage is empty', async () => {
    stubDashboard(() => jsonResponse(200, { vehicles: [] }))

    renderApp(paths.dashboard)

    expect(await screen.findByText(ro.dashboard.empty)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: ro.garage.add })).toBeInTheDocument()
  })

  it('names each vehicle and how long its cover has left', async () => {
    stubDashboard(() => jsonResponse(200, garage(
      { type: 'RCA', status: 'ACTIVE', documentId: 'd1', validUntil: '2027-01-01', daysRemaining: 200 },
    )))

    renderApp(paths.dashboard)

    expect(await screen.findByRole('link', { name: 'Dacia Logan' })).toBeInTheDocument()
    expect(screen.getByText(
      ro.documents.state.active.many.replace('{{days}}', '200'),
    )).toBeInTheDocument()
  })

  /**
   * Since 1.0.2 the backend sends the extinguisher and the first-aid kit once
   * somebody has entered them; each arrives as a line like any other and is
   * named in the reader's language.
   */
  it('names the equipment as it names the documents', async () => {
    stubDashboard(() => jsonResponse(200, garage(
      { type: 'EXTINGUISHER', status: 'EXPIRING_SOON', documentId: 'd9', validUntil: '2026-10-12', daysRemaining: 20 },
    )))

    renderApp(paths.dashboard)

    expect(await screen.findByText(ro.documents.type.EXTINGUISHER)).toBeInTheDocument()
    expect(screen.getByText(ro.documents.state.soon.many.replace('{{days}}', '20'))).toBeInTheDocument()
  })

  /**
   * The common case for a new account, and the reason every vehicle carries a
   * link: four lines saying nothing is set up would be an alarm without one, and
   * an invitation with it.
   */
  it('says what is not configured and offers the screen that configures it', async () => {
    stubDashboard(() => jsonResponse(200, garage(
      { type: 'CASCO', status: 'NOT_CONFIGURED' },
    )))

    renderApp(paths.dashboard)

    expect(await screen.findByText(ro.documents.state.notConfigured)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: ro.dashboard.configure }))
      .toHaveAttribute('href', paths.documents('v1'))
  })

  /**
   * The whole card opens the vehicle (2026-09-14). jsdom has no layout to tap,
   * so the tap itself was checked in a browser; what this holds is the markup
   * the stylesheet stretches: a marked card whose heading is the one link to the
   * vehicle, and a documents link that is a separate link to its own screen.
   */
  it('makes the card one way to the vehicle, with the documents link still its own', async () => {
    stubDashboard(() => jsonResponse(200, garage(
      { type: 'RCA', status: 'NOT_CONFIGURED' },
    )))

    renderApp(paths.dashboard)

    const name = await screen.findByRole('link', { name: 'Dacia Logan' })
    const card = name.closest('[data-card-link]')

    expect(card, 'the card is not marked for the stretched link').not.toBeNull()
    expect(name.parentElement?.tagName).toBe('H2')
    expect(name).toHaveAttribute('href', paths.vehicle('v1'))

    const links = within(card as HTMLElement).getAllByRole('link')
    expect(links.map(link => link.getAttribute('href')))
      .toEqual([paths.vehicle('v1'), paths.documents('v1')])
  })

  /**
   * The make's emblem on the right of the card (2026-09-14). Loaded on demand, so
   * it arrives after the card; the fixture's Dacia is one Simple Icons carries.
   */
  it('shows the emblem of a make it knows', async () => {
    stubDashboard(() => jsonResponse(200, garage(
      { type: 'RCA', status: 'NOT_CONFIGURED' },
    )))

    renderApp(paths.dashboard)

    const card = (await screen.findByRole('link', { name: 'Dacia Logan' })).closest('section')
    await waitFor(() => {
      expect(card?.querySelector('svg[data-brand-mark]')).not.toBeNull()
    })
  })

  /** The owner's own photograph beside the name, since 1.0.2. */
  it('shows the photograph on the card of a vehicle that has one', async () => {
    stubDashboard(() => jsonResponse(200, photographed(
      garage({ type: 'RCA', status: 'NOT_CONFIGURED' }),
    )))

    renderApp(paths.dashboard)

    const card = (await screen.findByRole('link', { name: 'Dacia Logan' })).closest('section')

    await waitFor(() => {
      expect(card?.querySelector('[data-card-head] img'))
        .toHaveAttribute('src', 'blob:card-photograph')
    })
  })

  /** And the circle stays empty where there is none, rather than the card losing a row. */
  it('keeps the circle empty for a vehicle nobody has photographed', async () => {
    stubDashboard(() => jsonResponse(200, garage({ type: 'RCA', status: 'NOT_CONFIGURED' })))

    renderApp(paths.dashboard)

    const card = (await screen.findByRole('link', { name: 'Dacia Logan' })).closest('section')

    expect(card?.querySelector('[data-thumbnail="empty"]')).not.toBeNull()
    expect(card?.querySelector('img')).toBeNull()
  })

  /**
   * Section 11 in its own words: the gap is the answer, and the policy already
   * bought is a second fact beside it rather than a replacement for it.
   */
  it('reads a lapse as the gap and still names when cover resumes', async () => {
    stubDashboard(() => jsonResponse(200, garage(
      {
        type: 'RCA', status: 'EXPIRED', documentId: 'd1',
        validUntil: '2026-08-12', daysRemaining: -5, upcomingFrom: '2026-09-01',
      },
    )))

    renderApp(paths.dashboard)

    expect(await screen.findByText(
      ro.documents.state.lapsedUntil.few
        .replace('{{days}}', '5')
        .replace('{{date}}', '1 septembrie 2026'),
    )).toBeInTheDocument()
  })

  /** A policy bought and not yet started must not read as one that ran out. */
  it('tells a policy that has not started apart from one that lapsed', async () => {
    stubDashboard(() => jsonResponse(200, garage(
      { type: 'ITP', status: 'EXPIRED', upcomingFrom: '2026-09-01' },
    )))

    renderApp(paths.dashboard)

    expect(await screen.findByText(
      ro.documents.state.startsOn
        .replace('{{date}}', '1 septembrie 2026'),
    )).toBeInTheDocument()
  })

  /**
   * 1.1: an instalment due within the week arrives under the documents, named
   * and toned as a document line is. A backend that sends none leaves the card
   * as it was.
   */
  it('lists an instalment due soon under the documents, with how far off it is', async () => {
    const view = garage(
      { type: 'RCA', status: 'ACTIVE', documentId: 'd1', validUntil: '2027-01-01', daysRemaining: 200 },
    )
    stubDashboard(() => jsonResponse(200, {
      vehicles: view.vehicles.map(vehicle => ({
        ...vehicle,
        payments: [{
          kind: 'LOAN', paymentId: 'p1', dueDate: '2026-10-15',
          daysRemaining: 3, instalment: 4, instalments: 36,
        }],
      })),
    }))

    renderApp(paths.dashboard)

    const line = (await screen.findByText(ro.payments.kind.LOAN)).closest('li')
    expect(line).not.toBeNull()
    expect(line).toHaveAttribute('data-tone', 'soon')
    expect(line).toHaveTextContent(
      ro.payments.due.few.replace('{{days}}', '3').replace('{{date}}', '15 octombrie 2026'),
    )
  })

  it('shows no instalment line when none is due', async () => {
    stubDashboard(() => jsonResponse(200, garage(
      { type: 'RCA', status: 'ACTIVE', documentId: 'd1', validUntil: '2027-01-01', daysRemaining: 200 },
    )))

    renderApp(paths.dashboard)

    await screen.findByRole('link', { name: 'Dacia Logan' })
    expect(screen.queryByText(ro.payments.kind.LOAN)).toBeNull()
    expect(screen.queryByText(ro.payments.kind.CASCO)).toBeNull()
  })
})
