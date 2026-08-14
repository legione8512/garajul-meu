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

describe('forgot password', () => {
  it('refuses an empty address without touching the network', async () => {
    renderApp(paths.forgotPassword)
    await screen.findByRole('heading', { level: 1, name: ro.screens.forgotPassword })

    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)

    await userEvent.click(screen.getByRole('button', { name: ro.forgotPassword.submit }))

    expect(screen.getByLabelText(ro.fields.email)).toHaveAccessibleDescription(ro.validation.required)
    expect(fetchMock).not.toHaveBeenCalled()
  })

  /**
   * The backend answers 204 for an unknown address exactly as for a known one,
   * so this screen has no way to tell them apart - and must not appear to.
   */
  it('moves on to the reset screen carrying the address', async () => {
    renderApp(paths.forgotPassword)
    await screen.findByRole('heading', { level: 1, name: ro.screens.forgotPassword })

    vi.stubGlobal('fetch', vi.fn(() => Promise.resolve(new Response(null, { status: 204 }))))

    await userEvent.type(screen.getByLabelText(ro.fields.email), 'marius@example.com')
    await userEvent.click(screen.getByRole('button', { name: ro.forgotPassword.submit }))

    expect(await screen.findByRole('heading', { level: 1, name: ro.screens.resetPassword })).toBeInTheDocument()
    expect(screen.getByLabelText(ro.fields.email)).toHaveValue('marius@example.com')
  })

  /** Five per hour on this endpoint, so this is an outcome people will meet. */
  it('reports having asked too many times', async () => {
    renderApp(paths.forgotPassword)
    await screen.findByRole('heading', { level: 1, name: ro.screens.forgotPassword })

    vi.stubGlobal('fetch', vi.fn(() => Promise.resolve(
      jsonResponse(429, { code: 'RATE_LIMITED', fieldErrors: [] }),
    )))

    await userEvent.type(screen.getByLabelText(ro.fields.email), 'marius@example.com')
    await userEvent.click(screen.getByRole('button', { name: ro.forgotPassword.submit }))

    expect(await screen.findByRole('alert')).toHaveTextContent(ro.errors.RATE_LIMITED)
  })
})