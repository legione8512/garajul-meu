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

describe('dashboard', () => {
  /** Nothing is fetched: the vehicle domain does not exist until Phase 7. */
  it('says there is nothing to show yet', async () => {
    vi.stubGlobal('fetch', vi.fn((input: string) => Promise.resolve(
      input.includes('/auth/refresh')
        ? jsonResponse(200, { accessToken: 'fresh', expiresInSeconds: 600, refreshToken: null })
        : jsonResponse(200, {
            id: '1', fullName: 'Marius Robert', email: 'marius@example.com',
            preferredLanguage: 'ro', timezone: 'Europe/Bucharest', emailVerified: true,
          }),
    )))

    renderApp(paths.dashboard)

    expect(await screen.findByText(ro.dashboard.empty)).toBeInTheDocument()
  })
})