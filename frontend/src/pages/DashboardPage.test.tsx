import { screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

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
      documents,
    }],
  }
}

describe('dashboard', () => {
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
      ro.documents.state.active.replace('{{days}}', '200'),
    )).toBeInTheDocument()
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
      ro.documents.state.lapsedUntil
        .replace('{{days}}', '5')
        .replace('{{date}}', new Date('2026-09-01').toLocaleDateString('ro')),
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
        .replace('{{date}}', new Date('2026-09-01').toLocaleDateString('ro')),
    )).toBeInTheDocument()
  })
})