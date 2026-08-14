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

describe('email verification', () => {
  it('starts with the address the previous screen handed over', async () => {
    renderApp(paths.verifyEmail, { email: 'marius@example.com' })

    expect(await screen.findByLabelText(ro.fields.email)).toHaveValue('marius@example.com')
  })

  /** Route state does not survive a reload, so the field has to be usable alone. */
  it('starts empty and still works when there was no handover', async () => {
    renderApp(paths.verifyEmail)

    expect(await screen.findByLabelText(ro.fields.email)).toHaveValue('')
  })

  it('confirms the address and sends the person on to sign in', async () => {
    renderApp(paths.verifyEmail, { email: 'marius@example.com' })
    await screen.findByRole('heading', { level: 1, name: ro.screens.verifyEmail })

    vi.stubGlobal('fetch', vi.fn(() => Promise.resolve(new Response(null, { status: 204 }))))

    await userEvent.type(screen.getByLabelText(ro.fields.code), '123456')
    await userEvent.click(screen.getByRole('button', { name: ro.verifyEmail.submit }))

    expect(await screen.findByRole('heading', { level: 1, name: ro.screens.login })).toBeInTheDocument()
  })

  it('reports a code the backend refuses', async () => {
    renderApp(paths.verifyEmail, { email: 'marius@example.com' })
    await screen.findByRole('heading', { level: 1, name: ro.screens.verifyEmail })

    vi.stubGlobal('fetch', vi.fn(() => Promise.resolve(
      jsonResponse(400, { code: 'VERIFICATION_CODE_INVALID', fieldErrors: [] }),
    )))

    await userEvent.type(screen.getByLabelText(ro.fields.code), '000000')
    await userEvent.click(screen.getByRole('button', { name: ro.verifyEmail.submit }))

    expect(await screen.findByRole('alert')).toHaveTextContent(ro.errors.VERIFICATION_CODE_INVALID)
  })

  it('sends another code and says so', async () => {
    renderApp(paths.verifyEmail, { email: 'marius@example.com' })
    await screen.findByRole('heading', { level: 1, name: ro.screens.verifyEmail })

    vi.stubGlobal('fetch', vi.fn(() => Promise.resolve(new Response(null, { status: 204 }))))

    await userEvent.click(screen.getByRole('button', { name: ro.verifyEmail.resend }))

    expect(await screen.findByRole('status')).toHaveTextContent(ro.verifyEmail.resent)
  })
})