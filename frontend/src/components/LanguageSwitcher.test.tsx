import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import { languageStorageKey } from '../i18n/language.ts'
import { en } from '../i18n/locales/en.ts'
import { ro } from '../i18n/locales/ro.ts'
import { paths } from '../routes/paths.ts'
import { renderApp } from '../test/renderApp.tsx'

/**
 * The trigger, reached by its label rather than by a role.
 *
 * <p>`summary` has no ARIA role of its own. Browsers expose it as a disclosure
 * button, but the HTML-to-ARIA mapping Testing Library follows lists none, so
 * `getByRole('button')` does not find it. Its accessible name does - which is
 * why the label has to be named, since the two tests below run in English.
 */
const opener = (label: string = ro.language.label) =>
  screen.getByLabelText(new RegExp(label))

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
 * An account whose language is `preferredLanguage`, or nobody signed in at all
 * when that is null. Records the PATCH body, which is the whole point: the only
 * way to prove the account was told is to look at what was sent.
 */
function stubAccount(preferredLanguage: string | null): Sent {
  const sent: Sent = { profile: null }
  let profile = {
    id: '1',
    fullName: 'Marius Robert',
    email: 'marius@example.com',
    preferredLanguage,
    timezone: 'Europe/Bucharest',
    emailVerified: true,
  }

  vi.stubGlobal('fetch', vi.fn((input: string, init?: RequestInit) => {
    if (input.includes('/auth/refresh')) {
      return Promise.resolve(preferredLanguage === null
        ? new Response(null, { status: 401 })
        : jsonResponse(200, {
            accessToken: 'fresh', expiresInSeconds: 600, refreshToken: null,
          }))
    }
    if (init?.method === 'PATCH') {
      sent.profile = JSON.parse(init.body as string) as Record<string, unknown>
      profile = { ...profile, ...sent.profile }
      return Promise.resolve(jsonResponse(200, profile))
    }
    return Promise.resolve(jsonResponse(200, profile))
  }))

  return sent
}

describe('language switcher', () => {
  it('names each language in itself rather than in the current one', async () => {
    renderApp(paths.login)
    await screen.findByRole('heading', { level: 1, name: ro.screens.login })

    await userEvent.click(opener())

    expect(screen.getByRole('button', { name: 'Română' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'English' })).toBeInTheDocument()
  })

  /**
   * That the menu opens and closes at all, which no other test here can show.
   *
   * <p>jsdom carries no user-agent stylesheet, so a closed `details` still has
   * its contents in the tree: a query finds both options whether or not anything
   * was ever clicked. What jsdom does implement is the activation behaviour, so
   * the disclosure is asserted on the `open` property and the hiding is left to
   * the browser that actually does it.
   *
   * <p>Escape is the half of this the element does not provide, which is the
   * half worth a test.
   */
  it('opens on the trigger and closes on Escape', async () => {
    renderApp(paths.login)
    await screen.findByRole('heading', { level: 1, name: ro.screens.login })

    const details = opener().closest('details')

    expect(details?.open).toBe(false)

    await userEvent.click(opener())
    expect(details?.open).toBe(true)

    await userEvent.keyboard('{Escape}')
    expect(details?.open).toBe(false)
  })

  it('re-renders the page in the chosen language', async () => {
    renderApp(paths.login)

    expect(await screen.findByRole('heading', { level: 1, name: ro.screens.login })).toBeInTheDocument()

    await userEvent.click(opener())
    await userEvent.click(screen.getByRole('button', { name: 'English' }))

    expect(screen.getByRole('heading', { level: 1, name: en.screens.login })).toBeInTheDocument()
  })

  it('remembers the choice for the next visit', async () => {
    renderApp(paths.login)
    await screen.findByRole('heading', { level: 1, name: ro.screens.login })

    await userEvent.click(opener())
    await userEvent.click(screen.getByRole('button', { name: 'English' }))

    expect(localStorage.getItem(languageStorageKey)).toBe('en')
  })

  /**
   * The one that matters, and the one whose absence was the defect.
   *
   * <p>Signed in, a language that never reaches the account does not survive a
   * reload: `applyLanguageOf` puts `preferred_language` back over it, storage
   * included. So the assertion is on the request body rather than on the screen -
   * the screen looked right the whole time this was broken.
   *
   * <p>The account starts in English on purpose. That is both the situation the
   * defect was found in and the signal that the session has finished restoring:
   * switching before it had would prove nothing, since telling no account is the
   * correct answer while nobody is known to be signed in.
   */
  it('tells the account, so a signed-in choice outlasts the page', async () => {
    const sent = stubAccount('en')

    renderApp(paths.login)
    await screen.findByRole('heading', { level: 1, name: en.screens.login })

    await userEvent.click(opener(en.language.label))
    await userEvent.click(screen.getByRole('button', { name: 'Română' }))

    await waitFor(() => {
      expect(sent.profile).toEqual({ preferredLanguage: 'ro' })
    })
    expect(screen.getByRole('heading', { level: 1, name: ro.screens.login })).toBeInTheDocument()
  })

  it('tells no account when nobody is signed in', async () => {
    const sent = stubAccount(null)

    renderApp(paths.login)
    await screen.findByRole('heading', { level: 1, name: ro.screens.login })

    await userEvent.click(opener())
    await userEvent.click(screen.getByRole('button', { name: 'English' }))

    expect(screen.getByRole('heading', { level: 1, name: en.screens.login })).toBeInTheDocument()
    expect(sent.profile).toBeNull()
  })
})