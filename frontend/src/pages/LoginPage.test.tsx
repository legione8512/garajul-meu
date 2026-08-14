import { screen } from '@testing-library/react'
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

const PROFILE = {
  id: '11111111-1111-1111-1111-111111111111',
  fullName: 'Marius Robert',
  email: 'marius@example.com',
  preferredLanguage: 'ro',
  timezone: 'Europe/Bucharest',
  emailVerified: true,
}

async function fillIn(email: string, password: string) {
  await userEvent.type(screen.getByLabelText(ro.fields.email), email)
  await userEvent.type(screen.getByLabelText(ro.fields.password), password)
}

describe('sign in', () => {
  it('refuses an empty form without touching the network', async () => {
    renderApp(paths.login)
    await screen.findByRole('heading', { level: 1, name: ro.screens.login })

    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)

    await userEvent.click(screen.getByRole('button', { name: ro.login.submit }))

    // Asserted per field rather than by searching for the text: both fields are
    // empty, so both produce the same message, and what matters is that each
    // one is tied to its own input by aria-describedby.
    expect(screen.getByLabelText(ro.fields.email)).toHaveAccessibleDescription(ro.validation.required)
    expect(screen.getByLabelText(ro.fields.password)).toHaveAccessibleDescription(ro.validation.required)
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('signs in and lands on the welcome page', async () => {
    renderApp(paths.login)
    await screen.findByRole('heading', { level: 1, name: ro.screens.login })

    vi.stubGlobal('fetch', vi.fn((input: string) => Promise.resolve(
      input.includes('/auth/login')
        ? jsonResponse(200, { accessToken: 'fresh', expiresInSeconds: 600, refreshToken: null })
        : jsonResponse(200, PROFILE),
    )))

    await fillIn('marius@example.com', 'a-long-enough-password')
    await userEvent.click(screen.getByRole('button', { name: ro.login.submit }))

    expect(await screen.findByText(`Ești autentificat ca ${PROFILE.fullName}.`)).toBeInTheDocument()
  })

  it('shows a translated message when the credentials are refused', async () => {
    renderApp(paths.login)
    await screen.findByRole('heading', { level: 1, name: ro.screens.login })

    vi.stubGlobal('fetch', vi.fn(() => Promise.resolve(
      jsonResponse(401, { code: 'INVALID_CREDENTIALS', fieldErrors: [] }),
    )))

    await fillIn('marius@example.com', 'the-wrong-password')
    await userEvent.click(screen.getByRole('button', { name: ro.login.submit }))

    expect(await screen.findByRole('alert')).toHaveTextContent(ro.errors.INVALID_CREDENTIALS)
  })

  /** The server disagreeing about a field must land on that field, not in a banner. */
  it('puts a field error from the server on the field it belongs to', async () => {
    renderApp(paths.login)
    await screen.findByRole('heading', { level: 1, name: ro.screens.login })

    vi.stubGlobal('fetch', vi.fn(() => Promise.resolve(
      jsonResponse(400, {
        code: 'VALIDATION_ERROR',
        fieldErrors: [{ field: 'email', constraint: 'Email' }],
      }),
    )))

    await fillIn('marius@example.com', 'a-long-enough-password')
    await userEvent.click(screen.getByRole('button', { name: ro.login.submit }))

    const emailInput = await screen.findByLabelText(ro.fields.email)
    expect(emailInput).toHaveAttribute('aria-invalid', 'true')
    expect(emailInput).toHaveAccessibleDescription(ro.validation.email)
  })

  /** Without these a password manager cannot fill or offer to save the form. */
  it('tells the browser what the fields are for', async () => {
    renderApp(paths.login)

    expect(await screen.findByLabelText(ro.fields.email)).toHaveAttribute('autocomplete', 'email')
    expect(screen.getByLabelText(ro.fields.password)).toHaveAttribute('autocomplete', 'current-password')
  })
})