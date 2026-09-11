import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { ro } from '../i18n/locales/ro.ts'
import type { Push, PushPermission } from '../notifications/push.ts'
import { paths } from '../routes/paths.ts'
import { renderApp } from '../test/renderApp.tsx'

const seam = vi.hoisted(() => ({ push: null as Push | null }))

vi.mock('../notifications/push.ts', () => seam)

const PROFILE = {
  id: '1', fullName: 'Marius Robert', email: 'marius@example.com',
  preferredLanguage: 'ro', timezone: 'Europe/Bucharest', emailVerified: true,
}

/** What the server answers for a phone it now holds as able. */
const HELD = {
  id: 'd1', platform: 'IOS', deviceName: null,
  notificationsEnabled: true, tokenUpdatedAt: '2026-09-11T11:12:30Z',
}

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

/**
 * A phone whose permission answers whatever the dialog last said. After the
 * button, `reportDevice` asks the permission again - a phone that kept
 * answering "prompt" would make the registration report nothing, and the test
 * would be about the mock.
 */
function phone(initial: PushPermission, answer: PushPermission = initial): Push {
  let current = initial

  return {
    permission: () => Promise.resolve(current),
    request: () => {
      current = answer
      return Promise.resolve(answer)
    },
    token: () => Promise.resolve('an-fcm-token'),
    platform: () => Promise.resolve('IOS' as const),
  }
}

/**
 * Signed in, with the device endpoint behind switches the test holds rather
 * than a queue of answers: `AppLayout` sends its own launch report, so how many
 * registrations reach the endpoint depends on timing, and a test that counted
 * them would be counting a race.
 *
 * @param gated every registration waits until the test calls `open`
 * @param up    whether the endpoint accepts the registration or fails with 500
 */
function stubSignedIn({ gated = false, up = true } = {}) {
  let release = () => {}
  const gate = gated
    ? new Promise<void>((resolve) => { release = resolve })
    : Promise.resolve()
  const server = { up, open: () => { release() } }
  const registrations = vi.fn()

  vi.stubGlobal('fetch', vi.fn(async (input: string) => {
    if (input.includes('/auth/refresh')) {
      return jsonResponse(200, { accessToken: 'fresh', expiresInSeconds: 600, refreshToken: null })
    }
    if (input.includes('/api/v1/devices')) {
      registrations()
      await gate
      return server.up
        ? jsonResponse(200, HELD)
        : jsonResponse(500, { code: 'INTERNAL_ERROR', requestId: 'req-1' })
    }
    return jsonResponse(200, PROFILE)
  }))

  return { server, registrations }
}

describe('screen 18, notifications on this phone', () => {
  beforeEach(() => {
    seam.push = null
  })

  it('in a browser, says where notifications arrive and offers nothing to press', async () => {
    stubSignedIn()

    renderApp(paths.notifications)

    expect(await screen.findByText(ro.notifications.webOnly)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: ro.notifications.enable })).not.toBeInTheDocument()
  })

  /**
   * The finding of 2026-09-11. The screen used to say notifications were on
   * the moment the permission was granted, before the server had been told
   * anything - so a registration that failed, or never finished, read as
   * healthy.
   */
  it('says it is checking, and nothing more, until the server has answered', async () => {
    const { server } = stubSignedIn({ gated: true })
    seam.push = phone('granted')

    renderApp(paths.notifications)

    expect(await screen.findByText(ro.notifications.checking)).toBeInTheDocument()
    expect(screen.queryByText(ro.notifications.granted)).not.toBeInTheDocument()

    server.open()

    expect(await screen.findByText(ro.notifications.granted)).toBeInTheDocument()
    expect(screen.queryByText(ro.notifications.checking)).not.toBeInTheDocument()
  })

  it('does not say notifications are on when the server refused the phone, and offers to try again', async () => {
    const { server } = stubSignedIn({ up: false })
    seam.push = phone('granted')

    renderApp(paths.notifications)

    expect(await screen.findByText(ro.notifications.notActive)).toBeInTheDocument()
    expect(screen.getByRole('alert')).toBeInTheDocument()
    expect(screen.queryByText(ro.notifications.granted)).not.toBeInTheDocument()

    server.up = true
    await userEvent.click(screen.getByRole('button', { name: ro.common.retry }))

    expect(await screen.findByText(ro.notifications.granted)).toBeInTheDocument()
    expect(screen.queryByText(ro.notifications.notActive)).not.toBeInTheDocument()
  })

  it('after a yes, says it is turning notifications on, then that they are on', async () => {
    const { server } = stubSignedIn({ gated: true })
    seam.push = phone('prompt', 'granted')

    renderApp(paths.notifications)
    await userEvent.click(await screen.findByRole('button', { name: ro.notifications.enable }))

    expect(await screen.findByText(ro.notifications.enabling)).toBeInTheDocument()
    expect(screen.queryByText(ro.notifications.granted)).not.toBeInTheDocument()

    server.open()

    expect(await screen.findByText(ro.notifications.granted)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: ro.notifications.enable })).not.toBeInTheDocument()
  })

  /**
   * A refusal mints nothing and registers nothing - the rule `reportDevice`
   * keeps - and the screen offers no button that would pretend the dialog can
   * be raised a second time.
   */
  it('after a no, says where the setting lives, offers no button, and registers nothing', async () => {
    const { registrations } = stubSignedIn()
    seam.push = phone('prompt', 'denied')

    renderApp(paths.notifications)
    await userEvent.click(await screen.findByRole('button', { name: ro.notifications.enable }))

    expect(await screen.findByText(ro.notifications.denied)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: ro.notifications.enable })).not.toBeInTheDocument()
    expect(registrations).not.toHaveBeenCalled()
  })
})
