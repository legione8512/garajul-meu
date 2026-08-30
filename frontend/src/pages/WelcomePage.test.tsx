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

const PROFILE = {
  id: '1', fullName: 'Marius Robert', email: 'marius@example.com',
  preferredLanguage: 'ro', timezone: 'Europe/Bucharest', emailVerified: true,
}

describe('welcome', () => {
  it('offers both ways in when nobody is signed in', async () => {
    renderApp(paths.welcome)

    expect(await screen.findByRole('link', { name: ro.welcome.signIn })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: ro.welcome.createAccount })).toBeInTheDocument()
  })

  /**
   * The public landing page has nothing to offer somebody already signed in.
   *
   * <p>Answers by path and rejects anything else, the arrangement LoginPage's
   * stub arrived at the hard way: a catch-all returning the profile served the
   * dashboard a user account on 2026-08-17, and the crash appeared in this file
   * while the change was in another one. Asserted on the heading for the same
   * reason - this test is about where a signed-in visitor is sent, not about
   * what that screen renders.
   */
  it('sends a signed-in visitor to the dashboard', async () => {
    vi.stubGlobal('fetch', vi.fn((input: string) => {
      if (input.includes('/auth/refresh')) {
        return Promise.resolve(jsonResponse(200, {
          accessToken: 'fresh', expiresInSeconds: 600, refreshToken: null,
        }))
      }
      if (input.includes('/users/me')) {
        return Promise.resolve(jsonResponse(200, PROFILE))
      }
      if (input.includes('/api/v1/dashboard')) {
        return Promise.resolve(jsonResponse(200, { vehicles: [] }))
      }
      return Promise.reject(new Error(`unstubbed request: ${input}`))
    }))

    renderApp(paths.welcome)

    expect(await screen.findByRole('heading', { level: 1, name: ro.screens.dashboard }))
      .toBeInTheDocument()
  })

  /**
   * The whole reason the status has three values. The pending response is
   * released at the end: a refresh promise that never settles never frees the
   * single in-flight slot in refresh.ts.
   */
  it('offers neither while the session is still being restored', async () => {
    let release: ((response: Response) => void) | undefined
    const pending = new Promise<Response>((resolve) => { release = resolve })
    vi.stubGlobal('fetch', vi.fn(() => pending))

    renderApp(paths.welcome)

    await screen.findByRole('heading', { level: 1, name: ro.welcome.headline })
    expect(screen.queryByRole('link', { name: ro.welcome.signIn })).not.toBeInTheDocument()

    release?.(jsonResponse(401, { code: 'REFRESH_TOKEN_INVALID' }))

    expect(await screen.findByRole('link', { name: ro.welcome.signIn })).toBeInTheDocument()
  })
})