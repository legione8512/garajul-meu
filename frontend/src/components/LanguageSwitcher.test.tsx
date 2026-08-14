import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'

import { languageStorageKey } from '../i18n/language.ts'
import { en } from '../i18n/locales/en.ts'
import { ro } from '../i18n/locales/ro.ts'
import { AppRoutes } from '../routes/AppRoutes.tsx'
import { paths } from '../routes/paths.ts'

function renderApp() {
  render(
    <MemoryRouter initialEntries={[paths.login]}>
      <AppRoutes />
    </MemoryRouter>,
  )
}

describe('language switcher', () => {
  it('names each language in itself rather than in the current one', () => {
    renderApp()

    expect(screen.getByRole('option', { name: 'Română' })).toBeInTheDocument()
    expect(screen.getByRole('option', { name: 'English' })).toBeInTheDocument()
  })

  it('re-renders the page in the chosen language', async () => {
    renderApp()

    expect(screen.getByRole('heading', { level: 1, name: ro.screens.login })).toBeInTheDocument()

    await userEvent.selectOptions(screen.getByLabelText(ro.language.label), 'en')

    expect(screen.getByRole('heading', { level: 1, name: en.screens.login })).toBeInTheDocument()
  })

  it('remembers the choice for the next visit', async () => {
    renderApp()

    await userEvent.selectOptions(screen.getByLabelText(ro.language.label), 'en')

    expect(localStorage.getItem(languageStorageKey)).toBe('en')
  })
})