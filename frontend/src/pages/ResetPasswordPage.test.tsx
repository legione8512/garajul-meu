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

describe('reset password', () => {
  it('starts with the address the previous screen handed over', async () => {
    renderApp(paths.resetPassword, { email: 'marius@example.com' })

    expect(await screen.findByLabelText(ro.fields.email)).toHaveValue('marius@example.com')
  })

  it('refuses a new password the backend would reject, without asking it', async () => {
    renderApp(paths.resetPassword, { email: 'marius@example.com' })
    await screen.findByRole('heading', { level: 1, name: ro.screens.resetPassword })

    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)

    await userEvent.type(screen.getByLabelText(ro.fields.code), '123456')
    await userEvent.type(screen.getByLabelText(ro.fields.newPassword), 'short')
    await userEvent.click(screen.getByRole('button', { name: ro.resetPassword.submit }))

    expect(screen.getByLabelText(ro.fields.newPassword))
      .toHaveAccessibleDescription('Lungime minimă: 12 caractere.')
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('changes the password and sends the person on to sign in', async () => {
    renderApp(paths.resetPassword, { email: 'marius@example.com' })
    await screen.findByRole('heading', { level: 1, name: ro.screens.resetPassword })

    vi.stubGlobal('fetch', vi.fn(() => Promise.resolve(new Response(null, { status: 204 }))))

    await userEvent.type(screen.getByLabelText(ro.fields.code), '123456')
    await userEvent.type(screen.getByLabelText(ro.fields.newPassword), 'a-replacement-password')
    await userEvent.click(screen.getByRole('button', { name: ro.resetPassword.submit }))

    expect(await screen.findByRole('heading', { level: 1, name: ro.screens.login })).toBeInTheDocument()
  })

  /** Distinct from an invalid code on purpose: the client can offer a resend. */
  it('reports an expired code as its own outcome', async () => {
    renderApp(paths.resetPassword, { email: 'marius@example.com' })
    await screen.findByRole('heading', { level: 1, name: ro.screens.resetPassword })

    vi.stubGlobal('fetch', vi.fn(() => Promise.resolve(
      jsonResponse(400, { code: 'VERIFICATION_CODE_EXPIRED', fieldErrors: [] }),
    )))

    await userEvent.type(screen.getByLabelText(ro.fields.code), '123456')
    await userEvent.type(screen.getByLabelText(ro.fields.newPassword), 'a-replacement-password')
    await userEvent.click(screen.getByRole('button', { name: ro.resetPassword.submit }))

    expect(await screen.findByRole('alert')).toHaveTextContent(ro.errors.VERIFICATION_CODE_EXPIRED)
  })
})