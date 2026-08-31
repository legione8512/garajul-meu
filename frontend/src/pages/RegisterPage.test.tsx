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

/**
 * Read through a helper rather than indexed directly: a zero-argument mock is
 * typed as an empty tuple, so calls[0][1] does not exist as far as TypeScript
 * is concerned even though it is there at runtime.
 */
function bodyOf(call: unknown[]): Record<string, unknown> {
  return JSON.parse((call[1] as RequestInit).body as string) as Record<string, unknown>
}

async function fillIn(password: string) {
  await userEvent.type(screen.getByLabelText(ro.fields.fullName), 'Marius Robert')
  await userEvent.type(screen.getByLabelText(ro.fields.email), 'marius@example.com')
  await userEvent.type(screen.getByLabelText(ro.fields.password), password)
}

describe('create account', () => {
  it('refuses a password shorter than the backend would accept, without asking it', async () => {
    renderApp(paths.register)
    await screen.findByRole('heading', { level: 1, name: ro.screens.register })

    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)

    await fillIn('short')
    await userEvent.click(screen.getByRole('button', { name: ro.register.submit }))

    expect(screen.getByLabelText(ro.fields.password))
      .toHaveAccessibleDescription('Lungime minimă: 12 caractere.')
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('registers and moves on to verification carrying the address', async () => {
    renderApp(paths.register)
    await screen.findByRole('heading', { level: 1, name: ro.screens.register })

    vi.stubGlobal('fetch', vi.fn(() => Promise.resolve(new Response(null, { status: 201 }))))

    await fillIn('a-long-enough-password')
    await userEvent.click(screen.getByRole('button', { name: ro.register.submit }))

    expect(await screen.findByRole('heading', { level: 1, name: ro.screens.verifyEmail })).toBeInTheDocument()
    expect(screen.getByLabelText(ro.fields.email)).toHaveValue('marius@example.com')
  })

  it('reports an address that is already taken', async () => {
    renderApp(paths.register)
    await screen.findByRole('heading', { level: 1, name: ro.screens.register })

    vi.stubGlobal('fetch', vi.fn(() => Promise.resolve(
      jsonResponse(409, { code: 'EMAIL_ALREADY_EXISTS', fieldErrors: [] }),
    )))

    await fillIn('a-long-enough-password')
    await userEvent.click(screen.getByRole('button', { name: ro.register.submit }))

    expect(await screen.findByRole('alert')).toHaveTextContent(ro.errors.EMAIL_ALREADY_EXISTS)
  })

  /** The language is taken from the switcher, not from a field nobody would fill in. */
  it('creates the account in the language currently selected', async () => {
    renderApp(paths.register)
    await screen.findByRole('heading', { level: 1, name: ro.screens.register })

    await userEvent.click(screen.getByLabelText(new RegExp(ro.language.label)))
    await userEvent.click(screen.getByRole('button', { name: 'English' }))

    const fetchMock = vi.fn(() => Promise.resolve(new Response(null, { status: 201 })))
    vi.stubGlobal('fetch', fetchMock)

    await userEvent.type(screen.getByLabelText('Full name'), 'Marius Robert')
    await userEvent.type(screen.getByLabelText('Email address'), 'marius@example.com')
    await userEvent.type(screen.getByLabelText('Password'), 'a-long-enough-password')
    await userEvent.click(screen.getByRole('button', { name: 'Create account' }))

    expect(bodyOf(fetchMock.mock.calls[0]).preferredLanguage).toBe('en')
  })
})