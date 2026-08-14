import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { languageStorageKey } from '../i18n/language.ts'
import { en } from '../i18n/locales/en.ts'
import { ro } from '../i18n/locales/ro.ts'
import { paths } from '../routes/paths.ts'
import { renderApp } from '../test/renderApp.tsx'

describe('language switcher', () => {
  it('names each language in itself rather than in the current one', async () => {
    renderApp(paths.login)

    expect(await screen.findByRole('option', { name: 'Română' })).toBeInTheDocument()
    expect(screen.getByRole('option', { name: 'English' })).toBeInTheDocument()
  })

  it('re-renders the page in the chosen language', async () => {
    renderApp(paths.login)

    expect(await screen.findByRole('heading', { level: 1, name: ro.screens.login })).toBeInTheDocument()

    await userEvent.selectOptions(screen.getByLabelText(ro.language.label), 'en')

    expect(screen.getByRole('heading', { level: 1, name: en.screens.login })).toBeInTheDocument()
  })

  it('remembers the choice for the next visit', async () => {
    renderApp(paths.login)
    await screen.findByRole('heading', { level: 1, name: ro.screens.login })

    await userEvent.selectOptions(screen.getByLabelText(ro.language.label), 'en')

    expect(localStorage.getItem(languageStorageKey)).toBe('en')
  })
})