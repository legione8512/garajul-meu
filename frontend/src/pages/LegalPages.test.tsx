import { screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import { ro } from '../i18n/locales/ro.ts'
import { paths } from '../routes/paths.ts'
import { renderApp } from '../test/renderApp.tsx'

/**
 * One file for both screens, against the usual one-per-page convention. They are
 * two placeholders with the same shape and the same three claims to check, and
 * two files of two near-identical tests would say less while costing more to
 * read.
 *
 * <p>Nobody is signed in: the refresh is refused, so the status settles on
 * anonymous. That is the state these assertions are about - somebody deciding
 * whether to create an account has to be able to read both documents first.
 */
function stubSignedOut() {
  vi.stubGlobal('fetch', vi.fn(() => Promise.resolve(
    new Response(JSON.stringify({ code: 'REFRESH_TOKEN_INVALID' }), {
      status: 401,
      headers: { 'Content-Type': 'application/json' },
    }),
  )))
}

describe('legal pages', () => {
  it('the terms are readable without an account', async () => {
    stubSignedOut()

    renderApp(paths.terms)

    expect(await screen.findByRole('heading', { level: 1, name: ro.screens.terms }))
      .toBeInTheDocument()
    expect(screen.getByText(ro.legal.termsScope)).toBeInTheDocument()
  })

  it('the privacy policy is readable without an account', async () => {
    stubSignedOut()

    renderApp(paths.privacy)

    expect(await screen.findByRole('heading', { level: 1, name: ro.screens.privacy }))
      .toBeInTheDocument()
    expect(screen.getByText(ro.legal.privacyScope)).toBeInTheDocument()
  })

  /**
   * The assertion that keeps a placeholder from shipping. Section 24 makes both
   * documents release-blocking and section 35 has not settled the wording; a
   * page carrying plausible invented terms is the one that goes out unnoticed,
   * so the notice is asserted rather than trusted to stay there.
   */
  it('both say plainly that the wording is not final', async () => {
    stubSignedOut()

    renderApp(paths.terms)

    expect(await screen.findByRole('note')).toHaveTextContent(ro.legal.placeholder)
  })

  it('the privacy notice carries the same warning', async () => {
    stubSignedOut()

    renderApp(paths.privacy)

    expect(await screen.findByRole('note')).toHaveTextContent(ro.legal.placeholder)
  })

  /**
   * Reachable from every screen, on both sides of the sign-in boundary. A
   * privacy policy findable only from inside an account cannot be read by the
   * person deciding whether to create one.
   */
  it('both are linked from the footer of a public screen', async () => {
    stubSignedOut()

    renderApp(paths.welcome)

    expect(await screen.findByRole('link', { name: ro.legal.terms })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: ro.legal.privacy })).toBeInTheDocument()
  })
})