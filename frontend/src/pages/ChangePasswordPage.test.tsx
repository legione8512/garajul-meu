import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

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

interface Sent {
  body: Record<string, unknown> | null
}

/**
 * @param failure what the change-password endpoint answers instead of 204
 */
function stubSignedIn(failure?: { status: number, body: unknown }): Sent {
  const sent: Sent = { body: null }

  vi.stubGlobal('fetch', vi.fn((input: string, init?: RequestInit) => {
    if (input.includes('/auth/refresh')) {
      return Promise.resolve(jsonResponse(200, {
        accessToken: 'fresh', expiresInSeconds: 600, refreshToken: null,
      }))
    }
    if (input.includes('/auth/logout')) {
      return Promise.resolve(new Response(null, { status: 204 }))
    }
    if (input.includes('/change-password')) {
      sent.body = JSON.parse(init?.body as string) as Record<string, unknown>
      return Promise.resolve(failure === undefined
        ? new Response(null, { status: 204 })
        : jsonResponse(failure.status, failure.body))
    }
    return Promise.resolve(jsonResponse(200, PROFILE))
  }))

  return sent
}

describe('change password', () => {
  it('warns that every session ends before anything is typed', async () => {
    stubSignedIn()

    renderApp(paths.changePassword)

    expect(await screen.findByRole('heading', { level: 1, name: ro.screens.changePassword }))
      .toBeInTheDocument()
    expect(screen.getByText(ro.changePassword.warning)).toBeInTheDocument()
  })

  /** The bound the backend enforces, checked here so a short one costs no request. */
  it('refuses a new password shorter than the backend would accept, without asking it', async () => {
    const sent = stubSignedIn()

    renderApp(paths.changePassword)
    await screen.findByLabelText(ro.fields.currentPassword)

    await userEvent.type(screen.getByLabelText(ro.fields.currentPassword), 'whatever-it-was')
    await userEvent.type(screen.getByLabelText(ro.fields.newPassword), 'prea-scurta')
    await userEvent.click(screen.getByRole('button', { name: ro.changePassword.submit }))

    expect(await screen.findByText(ro.validation.minLength.replace('{{min}}', '12')))
      .toBeInTheDocument()
    expect(sent.body).toBeNull()
  })

  /**
   * The assertion that matters on this screen. The backend has just revoked
   * every refresh token including this one, so staying "signed in" would be a
   * session that dies without explanation a quarter of an hour later. Landing on
   * the sign-in screen is the honest account of what happened - and it arrives
   * through RequireAuth reacting to an anonymous status, not through a redirect
   * written here.
   */
  it('changes the password, sends both fields, and leaves the protected area', async () => {
    const sent = stubSignedIn()

    renderApp(paths.changePassword)
    await screen.findByLabelText(ro.fields.currentPassword)

    await userEvent.type(screen.getByLabelText(ro.fields.currentPassword), 'a-long-enough-password')
    await userEvent.type(screen.getByLabelText(ro.fields.newPassword), 'o-parola-noua-lunga')
    await userEvent.click(screen.getByRole('button', { name: ro.changePassword.submit }))

    expect(await screen.findByRole('heading', { level: 1, name: ro.screens.login }))
      .toBeInTheDocument()
    expect(sent.body).toEqual({
      currentPassword: 'a-long-enough-password',
      newPassword: 'o-parola-noua-lunga',
    })
  })

  it('a wrong current password is explained and the session survives', async () => {
    stubSignedIn({ status: 400, body: { code: 'INVALID_CURRENT_PASSWORD' } })

    renderApp(paths.changePassword)
    await screen.findByLabelText(ro.fields.currentPassword)

    await userEvent.type(screen.getByLabelText(ro.fields.currentPassword), 'gresita')
    await userEvent.type(screen.getByLabelText(ro.fields.newPassword), 'o-parola-noua-lunga')
    await userEvent.click(screen.getByRole('button', { name: ro.changePassword.submit }))

    expect(await screen.findByRole('alert'))
      .toHaveTextContent(ro.errors.INVALID_CURRENT_PASSWORD)
    expect(screen.getByRole('heading', { level: 1, name: ro.screens.changePassword }))
      .toBeInTheDocument()
  })
})