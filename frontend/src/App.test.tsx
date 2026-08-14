import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'

import { ro } from './i18n/locales/ro.ts'
import { AppRoutes } from './routes/AppRoutes.tsx'
import { paths } from './routes/paths.ts'

function renderAt(path: string) {
  render(
    <MemoryRouter initialEntries={[path]}>
      <AppRoutes />
    </MemoryRouter>,
  )
}

describe('routing', () => {
  /**
   * Asserted against the Romanian resource rather than a literal, so rewording
   * a screen title stays a one-file change and these tests keep answering the
   * only question they exist to answer: did the right page render.
   */
  it.each([
    [paths.welcome, ro.screens.welcome],
    [paths.register, ro.screens.register],
    [paths.verifyEmail, ro.screens.verifyEmail],
    [paths.login, ro.screens.login],
    [paths.forgotPassword, ro.screens.forgotPassword],
    [paths.resetPassword, ro.screens.resetPassword],
  ])('renders the page registered for %s', (path, heading) => {
    renderAt(path)

    expect(screen.getByRole('heading', { level: 1, name: heading })).toBeInTheDocument()
  })

  it('falls back to the not-found page for an address nobody registered', () => {
    renderAt('/no-such-page')

    expect(screen.getByRole('heading', { level: 1, name: ro.screens.notFound })).toBeInTheDocument()
  })

  /**
   * Asserted on the not-found page specifically. The shell being a layout route
   * is what puts a way home in front of somebody who mistyped an address.
   */
  it('keeps the shell and a way home even on an unknown address', () => {
    renderAt('/no-such-page')

    expect(screen.getByRole('banner')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: ro.app.name })).toHaveAttribute('href', paths.welcome)
  })
})