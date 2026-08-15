import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
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
      : input.includes('/auth/logout')
        ? new Response(null, { status: 204 })
        : jsonResponse(200, {
            id: '1', fullName: 'Marius Robert', email: 'marius@example.com',
            preferredLanguage: 'ro', timezone: 'Europe/Bucharest', emailVerified: true,
          }),
  )))
}

describe('profile', () => {
  /**
   * Scoped to main rather than the whole document. "Română" legitimately
   * appears twice - once as an option in the language switcher, which lives in
   * the header, and once as this account's setting. Searching the document
   * would find both and prove neither.
   */
  it('shows the account it belongs to, with the language named in itself', async () => {
    stubSignedIn()

    renderApp(paths.profile)

    const page = await screen.findByRole('main')

    expect(within(page).getByText('Marius Robert')).toBeInTheDocument()
    expect(within(page).getByText('marius@example.com')).toBeInTheDocument()
    expect(within(page).getByText('Română')).toBeInTheDocument()
  })

  /**
   * No navigation of its own: ending the session makes the status anonymous and
   * the gate does the rest. Asserted here so that stays true - a hand-written
   * redirect added later would race it.
   */
  it('signing out leaves the protected area', async () => {
    stubSignedIn()

    renderApp(paths.profile)
    await screen.findByRole('button', { name: ro.profile.signOut })

    await userEvent.click(screen.getByRole('button', { name: ro.profile.signOut }))

    expect(await screen.findByRole('heading', { level: 1, name: ro.screens.login })).toBeInTheDocument()
  })
})