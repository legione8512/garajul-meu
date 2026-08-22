import { screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import { dashboardPath } from '../api/endpoints/dashboard.ts'
import { vehiclesPath } from '../api/endpoints/vehicles.ts'
import { ro } from '../i18n/locales/ro.ts'
import { paths } from '../routes/paths.ts'
import { renderApp } from '../test/renderApp.tsx'

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

/**
 * Answers each endpoint with the shape that endpoint actually returns.
 *
 * <p>Until 2026-08-23 it answered everything except the refresh with the user
 * profile. That is the wrong payload for every screen these tests render, and it
 * survived purely on timing: the assertions finished before the page's own
 * request resolved, so the bad data was never used. On a slower machine it does
 * resolve in time, and DashboardPage reads `data.vehicles.length` off a profile
 * — undefined — which throws and takes the whole tree with it, so the navigation
 * the test was waiting for disappears. It failed in CI having passed locally
 * every single time.
 *
 * <p>The lesson outlives the fix. **A stub that answers every URL the same way
 * is not a simplification, it is a fake contract.** These tests are about
 * routing and navigation and never look at a payload, which is precisely why
 * nobody noticed the payloads were nonsense — and why the failure, when it
 * arrived, arrived in a file that has nothing to do with the dashboard.
 */
function stubSignedIn() {
  vi.stubGlobal('fetch', vi.fn((input: string) => {
    if (input.includes('/auth/refresh')) {
      return Promise.resolve(jsonResponse(200, {
        accessToken: 'fresh', expiresInSeconds: 600, refreshToken: null,
      }))
    }

    if (input.includes(dashboardPath)) {
      return Promise.resolve(jsonResponse(200, { vehicles: [] }))
    }

    if (input.includes(vehiclesPath)) {
      return Promise.resolve(jsonResponse(200, []))
    }

    return Promise.resolve(jsonResponse(200, {
      id: '1', fullName: 'Marius Robert', email: 'marius@example.com',
      preferredLanguage: 'ro', timezone: 'Europe/Bucharest', emailVerified: true,
    }))
  }))
}

describe('protected routes', () => {
  it('sends somebody without a session to sign in', async () => {
    renderApp(paths.garage)

    expect(await screen.findByRole('heading', { level: 1, name: ro.screens.login })).toBeInTheDocument()
  })

  /**
   * Neither the page nor the sign-in form while the silent refresh is still in
   * flight. Redirecting here would tell somebody who is signed in that they are
   * not, and a cold Neon start makes that window seconds long, not milliseconds.
   *
   * <p>The pending response is released at the end rather than left hanging.
   * refresh.ts keeps one in-flight promise so concurrent callers share it, and
   * one that never settles never frees that slot - which wedges every later
   * test in this file, and in a browser would wedge every later refresh.
   */
  it('waits, visibly, while the session is still unknown', async () => {
    let release: ((response: Response) => void) | undefined
    const pending = new Promise<Response>((resolve) => { release = resolve })
    vi.stubGlobal('fetch', vi.fn(() => pending))

    renderApp(paths.garage)

    expect(await screen.findByRole('status')).toHaveTextContent(ro.common.loading)
    expect(screen.queryByRole('heading', { level: 1, name: ro.screens.login })).not.toBeInTheDocument()
    expect(screen.queryByRole('heading', { level: 1, name: ro.screens.garage })).not.toBeInTheDocument()

    release?.(jsonResponse(401, { code: 'REFRESH_TOKEN_INVALID' }))

    expect(await screen.findByRole('heading', { level: 1, name: ro.screens.login })).toBeInTheDocument()
  })

  it('lets a signed-in visitor through', async () => {
    stubSignedIn()

    renderApp(paths.garage)

    expect(await screen.findByRole('heading', { level: 1, name: ro.screens.garage })).toBeInTheDocument()
  })
})

describe('primary navigation', () => {
  it('offers exactly the three destinations section 5 fixes', async () => {
    stubSignedIn()

    renderApp(paths.dashboard)

    const nav = await screen.findByRole('navigation', { name: ro.navigation.label })
    expect(nav).toBeInTheDocument()
    expect(screen.getByRole('link', { name: ro.navigation.home })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: ro.navigation.garage })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: ro.navigation.profile })).toBeInTheDocument()
  })

  /** Without this a screen reader announces three identical links and no position. */
  it('marks the destination currently open', async () => {
    stubSignedIn()

    renderApp(paths.garage)

    expect(await screen.findByRole('link', { name: ro.navigation.garage }))
      .toHaveAttribute('aria-current', 'page')
    expect(screen.getByRole('link', { name: ro.navigation.home }))
      .not.toHaveAttribute('aria-current')
  })

  it('is absent on the public side', async () => {
    renderApp(paths.login)

    await screen.findByRole('heading', { level: 1, name: ro.screens.login })
    expect(screen.queryByRole('navigation', { name: ro.navigation.label })).not.toBeInTheDocument()
  })
})