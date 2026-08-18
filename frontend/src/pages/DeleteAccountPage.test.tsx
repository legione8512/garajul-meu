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
  deletes: number
  body: Record<string, unknown> | null
}

function stubSignedIn(failure?: { status: number, body: unknown }): Sent {
  const sent: Sent = { deletes: 0, body: null }

  vi.stubGlobal('fetch', vi.fn((input: string, init?: RequestInit) => {
    if (input.includes('/auth/refresh')) {
      return Promise.resolve(jsonResponse(200, {
        accessToken: 'fresh', expiresInSeconds: 600, refreshToken: null,
      }))
    }
    if (input.includes('/auth/logout')) {
      return Promise.resolve(new Response(null, { status: 204 }))
    }
    if (init?.method === 'DELETE') {
      sent.deletes += 1
      sent.body = JSON.parse(init.body as string) as Record<string, unknown>
      return Promise.resolve(failure === undefined
        ? new Response(null, { status: 204 })
        : jsonResponse(failure.status, failure.body))
    }
    return Promise.resolve(jsonResponse(200, PROFILE))
  }))

  return sent
}

describe('delete account', () => {
  it('says what is lost before asking for anything', async () => {
    stubSignedIn()

    renderApp(paths.deleteAccount)

    expect(await screen.findByRole('heading', { level: 1, name: ro.screens.deleteAccount }))
      .toBeInTheDocument()
    expect(screen.getByText(ro.deleteAccount.warning)).toBeInTheDocument()
  })

  /**
   * Two obstacles guarding different things: the password stops a stolen access
   * token, the confirmation stops the person's own hand. This is the second one,
   * and the count is what proves nothing left for the backend.
   */
  it('asks again before it deletes', async () => {
    const sent = stubSignedIn()

    renderApp(paths.deleteAccount)
    await screen.findByLabelText(ro.fields.currentPassword)

    await userEvent.type(screen.getByLabelText(ro.fields.currentPassword), 'a-long-enough-password')
    await userEvent.click(screen.getByRole('button', { name: ro.deleteAccount.submit }))

    expect(screen.getByText(ro.deleteAccount.confirm)).toBeInTheDocument()
    expect(sent.deletes).toBe(0)
  })

  it('changing your mind deletes nothing', async () => {
    const sent = stubSignedIn()

    renderApp(paths.deleteAccount)
    await screen.findByLabelText(ro.fields.currentPassword)

    await userEvent.type(screen.getByLabelText(ro.fields.currentPassword), 'a-long-enough-password')
    await userEvent.click(screen.getByRole('button', { name: ro.deleteAccount.submit }))
    await userEvent.click(screen.getByRole('button', { name: ro.deleteAccount.cancel }))

    expect(screen.queryByText(ro.deleteAccount.confirm)).not.toBeInTheDocument()
    expect(sent.deletes).toBe(0)
  })

  /**
   * The password travels in a body on a DELETE. Asserted on the body rather than
   * on the call alone, because the project state records that some proxies strip
   * it - and if that ever happens in production, this test is the reference for
   * what the client was actually sending.
   */
  it('confirming sends the password and leaves the protected area', async () => {
    const sent = stubSignedIn()

    renderApp(paths.deleteAccount)
    await screen.findByLabelText(ro.fields.currentPassword)

    await userEvent.type(screen.getByLabelText(ro.fields.currentPassword), 'a-long-enough-password')
    await userEvent.click(screen.getByRole('button', { name: ro.deleteAccount.submit }))
    await userEvent.click(screen.getByRole('button', { name: ro.deleteAccount.submit }))

    expect(await screen.findByRole('heading', { level: 1, name: ro.screens.login }))
      .toBeInTheDocument()
    expect(sent.deletes).toBe(1)
    expect(sent.body).toEqual({ currentPassword: 'a-long-enough-password' })
  })
})