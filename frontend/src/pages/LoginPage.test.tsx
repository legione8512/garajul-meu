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

/**
 * Answers by path, with no catch-all standing in for a screen that was not
 * thought about.
 *
 * <p>This used to end in a ternary whose last branch returned the profile for
 * anything that was not an auth call, and it broke the day the garage started
 * fetching: the garage received a user profile, found no length on it, and
 * rendered a heading and nothing else. The failure appeared in this file while
 * the change was in another one.
 */
function stubSuccessfulSignIn() {
  vi.stubGlobal('fetch', vi.fn((input: string) => {
    if (input.includes('/auth/login')) {
      return Promise.resolve(jsonResponse(200, {
        accessToken: 'fresh', expiresInSeconds: 600, refreshToken: null,
      }))
    }
    if (input.includes('/auth/refresh')) {
      return Promise.resolve(jsonResponse(401, { code: 'REFRESH_TOKEN_INVALID' }))
    }
    if (input.includes('/api/v1/vehicles')) {
      return Promise.resolve(jsonResponse(200, []))
    }
    return Promise.resolve(jsonResponse(200, PROFILE))
  }))
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

  it('signs in and lands on the dashboard', async () => {
    renderApp(paths.login)
    await screen.findByRole('heading', { level: 1, name: ro.screens.login })

    stubSuccessfulSignIn()

    await fillIn('marius@example.com', 'a-long-enough-password')
    await userEvent.click(screen.getByRole('button', { name: ro.login.submit }))

    expect(await screen.findByText(ro.dashboard.empty)).toBeInTheDocument()
  })

  /**
   * Otherwise a link to /garage sent to somebody signed out always opens the
   * dashboard instead, and nothing explains why.
   *
   * <p>Asserted on the heading rather than on the garage's empty state. This
   * test is about where signing in lands, and tying it to what that screen
   * happens to render couples it to a page it is not testing - which is exactly
   * how it broke once.
   */
  it('returns to the address that turned them away', async () => {
    renderApp(paths.login, { from: paths.garage })
    await screen.findByRole('heading', { level: 1, name: ro.screens.login })

    stubSuccessfulSignIn()

    await fillIn('marius@example.com', 'a-long-enough-password')
    await userEvent.click(screen.getByRole('button', { name: ro.login.submit }))

    expect(await screen.findByRole('heading', { level: 1, name: ro.screens.garage })).toBeInTheDocument()
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

  it('starts with the address a previous screen handed over', async () => {
    renderApp(paths.login, { email: 'marius@example.com' })

    expect(await screen.findByLabelText(ro.fields.email)).toHaveValue('marius@example.com')
  })
})