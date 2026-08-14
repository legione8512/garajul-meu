import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { setAccessToken } from '../api/tokenStore.ts'
import i18n from '../i18n/config.ts'
import { AuthProvider } from './AuthProvider.tsx'
import { useAuth } from './useAuth.ts'

afterEach(() => {
  vi.unstubAllGlobals()
  setAccessToken(null)
})

const PROFILE = {
  id: '11111111-1111-1111-1111-111111111111',
  fullName: 'Marius Robert',
  email: 'marius@example.com',
  preferredLanguage: 'ro',
  timezone: 'Europe/Bucharest',
  emailVerified: true,
}

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

/** A live session: the refresh works and the profile loads. */
function stubSignedIn(profile: unknown = PROFILE) {
  vi.stubGlobal('fetch', vi.fn((input: string) => Promise.resolve(
    input.includes('/auth/refresh')
      ? jsonResponse(200, { accessToken: 'fresh', expiresInSeconds: 600, refreshToken: null })
      : jsonResponse(200, profile),
  )))
}

function stubSignedOut() {
  vi.stubGlobal('fetch', vi.fn(() => Promise.resolve(
    jsonResponse(401, { code: 'REFRESH_TOKEN_INVALID' }),
  )))
}

function Probe() {
  const { status, profile, signIn, signOut } = useAuth()

  return (
    <>
      <p>{`${status}:${profile?.fullName ?? '-'}`}</p>
      <button type="button" onClick={() => { void signIn('marius@example.com', 'a-long-enough-password') }}>
        in
      </button>
      <button type="button" onClick={() => { void signOut() }}>out</button>
    </>
  )
}

function renderProvider() {
  render(<AuthProvider><Probe /></AuthProvider>)
}

describe('authentication context', () => {
  it('restores a session the refresh cookie still supports', async () => {
    stubSignedIn()
    renderProvider()

    expect(await screen.findByText('authenticated:Marius Robert')).toBeInTheDocument()
  })

  it('settles on anonymous when there is no session to restore', async () => {
    stubSignedOut()
    renderProvider()

    expect(await screen.findByText('anonymous:-')).toBeInTheDocument()
  })

  /**
   * The state that stops a signed-in person being shown a signed-out screen for
   * a moment while the silent refresh is still in flight.
   */
  it('reports unknown until the restore attempt settles', async () => {
    stubSignedOut()
    renderProvider()

    expect(screen.getByText('unknown:-')).toBeInTheDocument()

    await screen.findByText('anonymous:-')
  })

  /** Section 6: the account's language follows the person, not the device. */
  it('adopts the language stored on the account', async () => {
    stubSignedIn({ ...PROFILE, preferredLanguage: 'en' })
    renderProvider()

    await screen.findByText('authenticated:Marius Robert')
    await waitFor(() => {
      expect(i18n.resolvedLanguage).toBe('en')
    })
  })

  it('signs in and loads the profile', async () => {
    stubSignedOut()
    renderProvider()
    await screen.findByText('anonymous:-')

    stubSignedIn()
    await userEvent.click(screen.getByRole('button', { name: 'in' }))

    expect(await screen.findByText('authenticated:Marius Robert')).toBeInTheDocument()
  })

  it('signs out', async () => {
    stubSignedIn()
    renderProvider()
    await screen.findByText('authenticated:Marius Robert')

    vi.stubGlobal('fetch', vi.fn(() => Promise.resolve(new Response(null, { status: 204 }))))
    await userEvent.click(screen.getByRole('button', { name: 'out' }))

    expect(await screen.findByText('anonymous:-')).toBeInTheDocument()
  })
})