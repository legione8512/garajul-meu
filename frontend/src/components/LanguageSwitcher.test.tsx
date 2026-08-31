import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

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
 * `getByRole('button')` does not find it. Its accessible name does.
 */
const opener = () => screen.getByLabelText(new RegExp(ro.language.label))

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
})