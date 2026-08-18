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
  request: Record<string, unknown> | null
  confirm: Record<string, unknown> | null
}

function stubSignedIn(confirmFailure?: { status: number, body: unknown }): Sent {
  const sent: Sent = { request: null, confirm: null }

  vi.stubGlobal('fetch', vi.fn((input: string, init?: RequestInit) => {
    if (input.includes('/auth/refresh')) {
      return Promise.resolve(jsonResponse(200, {
        accessToken: 'fresh', expiresInSeconds: 600, refreshToken: null,
      }))
    }
    if (input.includes('/confirm-email-change')) {
      sent.confirm = JSON.parse(init?.body as string) as Record<string, unknown>
      if (confirmFailure !== undefined) {
        return Promise.resolve(jsonResponse(confirmFailure.status, confirmFailure.body))
      }
      // The address has moved and is unverified - the honest consequence of a
      // code that proved control of the old inbox only.
      return Promise.resolve(jsonResponse(200, {
        ...PROFILE, email: 'nou@example.com', emailVerified: false,
      }))
    }
    if (input.includes('/change-email')) {
      sent.request = JSON.parse(init?.body as string) as Record<string, unknown>
      return Promise.resolve(new Response(null, { status: 204 }))
    }
    return Promise.resolve(jsonResponse(200, PROFILE))
  }))

  return sent
}

async function requestTheChange(address = 'nou@example.com') {
  await screen.findByLabelText(ro.fields.newEmail)

  await userEvent.type(screen.getByLabelText(ro.fields.newEmail), address)
  await userEvent.type(screen.getByLabelText(ro.fields.currentPassword), 'a-long-enough-password')
  await userEvent.click(screen.getByRole('button', { name: ro.changeEmail.request }))
}

describe('change email', () => {
  /**
   * Said before anything is typed, because somebody who does not know it goes
   * looking in the new inbox, finds nothing, and concludes we are broken.
   */
  it('says the code goes to the current address, not the new one', async () => {
    stubSignedIn()

    renderApp(paths.changeEmail)

    expect(await screen.findByRole('heading', { level: 1, name: ro.screens.changeEmail }))
      .toBeInTheDocument()
    expect(screen.getByText(ro.changeEmail.instructions)).toBeInTheDocument()
  })

  it('refuses an address that is not one, without asking the backend', async () => {
    const sent = stubSignedIn()

    renderApp(paths.changeEmail)
    await screen.findByLabelText(ro.fields.newEmail)

    await userEvent.type(screen.getByLabelText(ro.fields.newEmail), 'nu-e-o-adresa')
    await userEvent.type(screen.getByLabelText(ro.fields.currentPassword), 'a-long-enough-password')
    await userEvent.click(screen.getByRole('button', { name: ro.changeEmail.request }))

    expect(await screen.findByText(ro.validation.email)).toBeInTheDocument()
    expect(sent.request).toBeNull()
  })

  it('requesting sends the address and the password, then asks for the code', async () => {
    const sent = stubSignedIn()

    renderApp(paths.changeEmail)
    await requestTheChange()

    expect(await screen.findByText(ro.changeEmail.codeSent)).toBeInTheDocument()
    expect(screen.getByLabelText(ro.fields.code)).toBeInTheDocument()
    expect(sent.request).toEqual({
      newEmail: 'nou@example.com',
      currentPassword: 'a-long-enough-password',
    })
  })

  /**
   * Confirming moves the address and leaves it unverified, and the screen says
   * so - a message claiming the change is complete would be wrong in the one way
   * that matters, since nothing will reach the new inbox until it is verified.
   */
  it('confirming moves the address and says it still needs verifying', async () => {
    const sent = stubSignedIn()

    renderApp(paths.changeEmail)
    await requestTheChange()
    await screen.findByLabelText(ro.fields.code)

    await userEvent.type(screen.getByLabelText(ro.fields.code), '123456')
    await userEvent.click(screen.getByRole('button', { name: ro.changeEmail.confirm }))

    expect(await screen.findByText(ro.changeEmail.done)).toBeInTheDocument()
    expect(sent.confirm).toEqual({ code: '123456' })
  })

  it('a wrong code is explained and the screen stays where it is', async () => {
    stubSignedIn({ status: 400, body: { code: 'VERIFICATION_CODE_INVALID' } })

    renderApp(paths.changeEmail)
    await requestTheChange()
    await screen.findByLabelText(ro.fields.code)

    await userEvent.type(screen.getByLabelText(ro.fields.code), '000000')
    await userEvent.click(screen.getByRole('button', { name: ro.changeEmail.confirm }))

    expect(await screen.findByRole('alert'))
      .toHaveTextContent(ro.errors.VERIFICATION_CODE_INVALID)
    expect(screen.queryByText(ro.changeEmail.done)).not.toBeInTheDocument()
  })
})