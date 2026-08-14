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

describe('welcome', () => {
  it('offers both ways in when nobody is signed in', async () => {
    renderApp(paths.welcome)

    expect(await screen.findByRole('link', { name: ro.welcome.signIn })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: ro.welcome.createAccount })).toBeInTheDocument()
  })

  it('greets the person and offers a way out when signed in', async () => {
    vi.stubGlobal('fetch', vi.fn((input: string) => Promise.resolve(
      input.includes('/auth/refresh')
        ? jsonResponse(200, { accessToken: 'fresh', expiresInSeconds: 600, refreshToken: null })
        : jsonResponse(200, {
            id: '1', fullName: 'Marius Robert', email: 'marius@example.com',
            preferredLanguage: 'ro', timezone: 'Europe/Bucharest', emailVerified: true,
          }),
    )))

    renderApp(paths.welcome)

    expect(await screen.findByText('Ești autentificat ca Marius Robert.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: ro.welcome.signOut })).toBeInTheDocument()
  })

  /**
   * The whole reason the status has three values. A refresh that never settles
   * must leave the page saying nothing about the session either way.
   */
  it('offers neither while the session is still being restored', async () => {
    vi.stubGlobal('fetch', vi.fn(() => new Promise<Response>(() => { /* never settles */ })))

    renderApp(paths.welcome)

    await screen.findByRole('heading', { level: 1, name: ro.screens.welcome })

    expect(screen.queryByRole('link', { name: ro.welcome.signIn })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: ro.welcome.signOut })).not.toBeInTheDocument()
  })
})