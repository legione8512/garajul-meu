import { screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import i18n from '../i18n/config.ts'
import { ro } from '../i18n/locales/ro.ts'
import { CONTACT_EMAIL, OPERATOR_NAME } from '../legal/document.ts'
import { termsAndConditions } from '../legal/terms.ts'
import { paths } from '../routes/paths.ts'
import { renderApp } from '../test/renderApp.tsx'

/**
 * One file for both screens, against the usual one-per-page convention. They are
 * two documents rendered by one component, with the same claims to check, and
 * two files of near-identical tests would say less while costing more to read.
 * The support contact on the features page is here too, because it is the same
 * address and the same promise.
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
    expect(screen.getByRole('heading', { level: 2, name: termsAndConditions.ro.sections[0].title }))
      .toBeInTheDocument()
    expect(screen.getByText(ro.legal.updated.replace('{{date}}', termsAndConditions.ro.updated)))
      .toBeInTheDocument()
  })

  it('the privacy policy is readable without an account', async () => {
    stubSignedOut()

    renderApp(paths.privacy)

    expect(await screen.findByRole('heading', { level: 1, name: ro.screens.privacy }))
      .toBeInTheDocument()
    expect(screen.getByRole('rowheader', { name: 'Railway (serverul aplicației)' }))
      .toBeInTheDocument()
  })

  /**
   * The placeholder that stood here until 2026-09-14 said the wording was not
   * final. Its replacement has to say who is responsible and how to reach them,
   * which is the first thing either document is for.
   */
  it('both name the operator and a contact address that can be written to', async () => {
    stubSignedOut()

    renderApp(paths.privacy)

    const contacts = await screen.findAllByRole('link', { name: CONTACT_EMAIL })
    expect(contacts[0]).toHaveAttribute('href', `mailto:${CONTACT_EMAIL}`)
    expect(screen.getAllByText(new RegExp(OPERATOR_NAME)).length).toBeGreaterThan(0)
    expect(screen.queryByRole('note')).toBeNull()
  })

  it('the terms are in English when the interface is', async () => {
    stubSignedOut()
    await i18n.changeLanguage('en')

    renderApp(paths.terms)

    expect(await screen.findByRole('heading', { level: 2, name: termsAndConditions.en.sections[0].title }))
      .toBeInTheDocument()
  })

  /** The support address App Store Connect is given is this page, so it must carry one. */
  it('the page about the application gives the contact address', async () => {
    stubSignedOut()

    renderApp(paths.features)

    expect(await screen.findByRole('heading', { level: 2, name: ro.features.contact.title }))
      .toBeInTheDocument()
    expect(screen.getByRole('link', { name: CONTACT_EMAIL })).toHaveAttribute('href', `mailto:${CONTACT_EMAIL}`)
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