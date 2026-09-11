import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import { ro } from '../i18n/locales/ro.ts'
import { paths } from '../routes/paths.ts'
import { renderApp } from '../test/renderApp.tsx'

const PROFILE = {
  id: '1', fullName: 'Marius Robert', email: 'marius@example.com',
  preferredLanguage: 'ro', timezone: 'Europe/Bucharest', emailVerified: true,
}

interface Sent {
  profile: Record<string, unknown> | null
}

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

/**
 * Keeps the account in memory so a PATCH is visible to whatever reads next, and
 * records the exact body. The notification preferences left this screen on
 * 2026-09-11, and their tests went with them to screen 18's.
 */
function stubAccount(profileStatus = 200): Sent {
  const sent: Sent = { profile: null }
  let profile = { ...PROFILE }

  vi.stubGlobal('fetch', vi.fn((input: string, init?: RequestInit) => {
    if (input.includes('/auth/refresh')) {
      return Promise.resolve(jsonResponse(200, {
        accessToken: 'fresh', expiresInSeconds: 600, refreshToken: null,
      }))
    }
    if (input.includes('/auth/logout')) {
      return Promise.resolve(new Response(null, { status: 204 }))
    }
    if (init?.method === 'PATCH') {
      sent.profile = JSON.parse(init.body as string) as Record<string, unknown>
      if (profileStatus !== 200) {
        return Promise.resolve(jsonResponse(profileStatus, { code: 'VALIDATION_ERROR' }))
      }
      profile = { ...profile, ...sent.profile }
      return Promise.resolve(jsonResponse(200, profile))
    }
    return Promise.resolve(jsonResponse(200, profile))
  }))

  return sent
}

describe('profile', () => {
  /**
   * Scoped to main rather than the whole document. The language switcher lives
   * in the header and carries the same label as the account's own setting, so
   * an unscoped query finds two selects and proves neither. Every test that
   * touches the language does this - the one that did not cost a run.
   */
  it('shows the account it belongs to, with the language named in itself', async () => {
    stubAccount()

    renderApp(paths.profile)

    const page = await screen.findByRole('main')

    expect(within(page).getByText(/marius@example\.com/)).toBeInTheDocument()
    expect(within(page).getByLabelText(ro.fields.fullName)).toHaveValue('Marius Robert')
    expect(within(page).getByLabelText(ro.language.label)).toHaveValue('ro')
    expect(within(page).getByLabelText(ro.fields.timezone)).toHaveValue('Europe/Bucharest')
  })

  it('saves the account details and says so', async () => {
    const sent = stubAccount()

    renderApp(paths.profile)
    await screen.findByLabelText(ro.fields.fullName)

    await userEvent.clear(screen.getByLabelText(ro.fields.fullName))
    await userEvent.type(screen.getByLabelText(ro.fields.fullName), 'Marius R.')
    await userEvent.click(screen.getByRole('button', { name: ro.profile.save }))

    expect(await screen.findByText(ro.profile.saved)).toBeInTheDocument()
    expect(sent.profile).toMatchObject({ fullName: 'Marius R.' })
  })

  /**
   * The language is an account setting, not a device one, so saving it has to
   * move the interface as well as the row. Asserted through a heading that is
   * translated, because a change reaching only the database is invisible until
   * the next sign-in.
   */
  it('changing the language re-renders the application in it', async () => {
    stubAccount()

    renderApp(paths.profile)

    const page = await screen.findByRole('main')

    await userEvent.selectOptions(within(page).getByLabelText(ro.language.label), 'en')
    await userEvent.click(within(page).getByRole('button', { name: ro.profile.save }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Profile' })).toBeInTheDocument()
  })

  it('a refused change is explained and nothing is claimed to have been saved', async () => {
    stubAccount(400)

    renderApp(paths.profile)
    await screen.findByLabelText(ro.fields.fullName)

    await userEvent.click(screen.getByRole('button', { name: ro.profile.save }))

    expect(await screen.findByRole('alert')).toHaveTextContent(ro.errors.VALIDATION_ERROR)
    expect(screen.queryByText(ro.profile.saved)).not.toBeInTheDocument()
  })

  /** An empty name is refused here, before it costs a request. */
  it('refuses an empty name without asking the backend', async () => {
    const sent = stubAccount()

    renderApp(paths.profile)
    await screen.findByLabelText(ro.fields.fullName)

    await userEvent.clear(screen.getByLabelText(ro.fields.fullName))
    await userEvent.click(screen.getByRole('button', { name: ro.profile.save }))

    expect(await screen.findByText(ro.validation.required)).toBeInTheDocument()
    expect(sent.profile).toBeNull()
  })

  /**
   * The preferences moved to screen 18 on 2026-09-11, beside what the phone
   * allows. The profile keeps the way there and nothing that could disagree
   * with it.
   */
  it('points to the notifications screen and carries no preferences of its own', async () => {
    stubAccount()

    renderApp(paths.profile)

    const way = await screen.findByRole('link', { name: ro.reminders.manage })
    expect(way).toHaveAttribute('href', paths.notifications)
    expect(screen.queryByLabelText(ro.notificationPreferences.enabled)).not.toBeInTheDocument()
  })

  /**
   * The developer's decision of 2026-09-11: the way to deleting the account is
   * painted as dangerous. jsdom draws no colours, so the test pins the marker
   * the stylesheet paints - and a return to the quiet link would fail it.
   */
  it('marks the way to deleting the account as destructive', async () => {
    stubAccount()

    renderApp(paths.profile)

    const way = await screen.findByRole('link', { name: ro.profile.deleteAccount })
    expect(way).toHaveAttribute('href', paths.deleteAccount)
    expect(way).toHaveAttribute('data-action', 'destructive')
  })

  /**
   * No navigation of its own: ending the session makes the status anonymous and
   * the gate does the rest. Asserted here so that stays true - a hand-written
   * redirect added later would race it.
   */
  it('signing out leaves the protected area', async () => {
    stubAccount()

    renderApp(paths.profile)
    await screen.findByRole('button', { name: ro.profile.signOut })

    await userEvent.click(screen.getByRole('button', { name: ro.profile.signOut }))

    expect(await screen.findByRole('heading', { level: 1, name: ro.screens.login })).toBeInTheDocument()
  })
})