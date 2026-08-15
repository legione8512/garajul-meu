import { screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import { ro } from '../i18n/locales/ro.ts'
import { paths } from '../routes/paths.ts'
import { renderApp } from '../test/renderApp.tsx'

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function stubSignedIn() {
  vi.stubGlobal('fetch', vi.fn((input: string) => Promise.resolve(
    input.includes('/auth/refresh')
      ? jsonResponse(200, { accessToken: 'fresh', expiresInSeconds: 600, refreshToken: null })
      : jsonResponse(200, {
          id: '1', fullName: 'Marius Robert', email: 'marius@example.com',
          preferredLanguage: 'ro', timezone: 'Europe/Bucharest', emailVerified: true,
        }),
  )))
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