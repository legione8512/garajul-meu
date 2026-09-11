import { screen, waitFor } from '@testing-library/react'
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

/** The account's preferences as screen 15 would show them; the switch is the first field. */
const PREFERENCES = {
  notificationsEnabled: true,
  remind30Days: true, remind14Days: true, remind7Days: true,
  remind3Days: false, remind1Day: false, remindOnExpiry: true,
  notificationLocalTime: '09:00:00',
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
 * <p>The preferences endpoint answers what the account holds, and records every
 * save, because the switch on this screen is the account's own setting.
 *
 * @param gated       every registration waits until the test calls `open`
 * @param up          whether the endpoint accepts the registration or fails with 500
 * @param remindersOn the account's switch as the server holds it
 */
function stubSignedIn({ gated = false, up = true, remindersOn = true } = {}) {
  let release = () => {}
  const gate = gated
    ? new Promise<void>((resolve) => { release = resolve })
    : Promise.resolve()
  const server = { up, saves: true, open: () => { release() } }
  const registrations = vi.fn()
  const saved: Record<string, unknown>[] = []

  vi.stubGlobal('fetch', vi.fn(async (input: string, init?: RequestInit) => {
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
    if (input.includes('/notification-preferences')) {
      if (init?.method === 'PUT') {
        const body = JSON.parse(init.body as string) as Record<string, unknown>
        saved.push(body)
        return server.saves
          ? jsonResponse(200, body)
          : jsonResponse(500, { code: 'INTERNAL_ERROR', requestId: 'req-2' })
      }
      return jsonResponse(200, { ...PREFERENCES, notificationsEnabled: remindersOn })
    }
    return jsonResponse(200, PROFILE)
  }))

  return { server, registrations, saved }
}

describe('screen 18, notifications on this phone', () => {
  beforeEach(() => {
    seam.push = null
  })

  it('in a browser, says where notifications arrive and asks for no permission', async () => {
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

  /**
   * The developer's request of 2026-09-11: stopping reminders from the screen
   * where somebody looks for it. The switch is the account's own setting - the
   * one screen 15 shows too - so it starts where the server says it is.
   */
  it('offers the account\'s switch once the phone is held, on while reminders are sent', async () => {
    stubSignedIn()
    seam.push = phone('granted')

    renderApp(paths.notifications)

    expect(await screen.findByRole('checkbox', { name: ro.notificationPreferences.enabled })).toBeChecked()
    expect(screen.getByText(ro.notifications.granted)).toBeInTheDocument()
  })

  /**
   * The endpoint is a replace and refuses a body that leaves a field out, so the
   * switch sends every preference back with only its own changed.
   */
  it('turning reminders off saves every preference with only that one changed, and says they are off', async () => {
    const { saved } = stubSignedIn()
    seam.push = phone('granted')

    renderApp(paths.notifications)
    await userEvent.click(await screen.findByRole('checkbox', { name: ro.notificationPreferences.enabled }))

    expect(await screen.findByText(ro.notifications.paused)).toBeInTheDocument()
    expect(screen.queryByText(ro.notifications.granted)).not.toBeInTheDocument()
    expect(screen.getByRole('checkbox', { name: ro.notificationPreferences.enabled })).not.toBeChecked()
    await waitFor(() => { expect(saved).toEqual([{ ...PREFERENCES, notificationsEnabled: false }]) })
  })

  /**
   * A phone the server holds is not enough to say notifications are on: with the
   * account's reminders off, nothing is sent to it.
   */
  it('says reminders are off rather than that notifications are on, and turns them back on', async () => {
    const { saved } = stubSignedIn({ remindersOn: false })
    seam.push = phone('granted')

    renderApp(paths.notifications)

    expect(await screen.findByText(ro.notifications.paused)).toBeInTheDocument()
    expect(screen.queryByText(ro.notifications.granted)).not.toBeInTheDocument()

    await userEvent.click(screen.getByRole('checkbox', { name: ro.notificationPreferences.enabled }))

    expect(await screen.findByText(ro.notifications.granted)).toBeInTheDocument()
    await waitFor(() => { expect(saved).toEqual([{ ...PREFERENCES, notificationsEnabled: true }]) })
  })

  /**
   * Showing "off" while the server still sends is the lie this screen was rebuilt
   * to stop telling, so a refused save puts the switch back and says why.
   */
  it('puts the switch back and says why when the server refuses the change', async () => {
    const { server } = stubSignedIn()
    server.saves = false
    seam.push = phone('granted')

    renderApp(paths.notifications)
    await userEvent.click(await screen.findByRole('checkbox', { name: ro.notificationPreferences.enabled }))

    expect(await screen.findByRole('alert')).toBeInTheDocument()
    await waitFor(() => {
      expect(screen.getByRole('checkbox', { name: ro.notificationPreferences.enabled })).toBeChecked()
    })
    expect(screen.getByText(ro.notifications.granted)).toBeInTheDocument()
    expect(screen.queryByText(ro.notifications.paused)).not.toBeInTheDocument()
  })

  /**
   * Moved here from screen 15 on 2026-09-11 with the preferences themselves. In
   * a browser there is no phone to ask, and the account's settings are still
   * worth setting from a computer.
   */
  it('in a browser, shows the account\'s stored preferences', async () => {
    stubSignedIn()

    renderApp(paths.notifications)

    expect(await screen.findByLabelText(ro.notificationPreferences.enabled)).toBeChecked()
    expect(screen.getByLabelText(ro.notificationPreferences.remind3Days)).not.toBeChecked()
    expect(screen.getByLabelText(ro.notificationPreferences.time)).toHaveValue('09:00')
  })

  /**
   * The endpoint is a replace and the backend refuses a body missing a switch,
   * so the assertion is on the whole body rather than on the one field touched.
   * The time goes back with its seconds, which is what a LocalTime expects.
   */
  it('saving the schedule sends all eight fields, not only the one changed', async () => {
    const { saved } = stubSignedIn()

    renderApp(paths.notifications)
    await userEvent.click(await screen.findByLabelText(ro.notificationPreferences.remind3Days))
    await userEvent.click(screen.getByRole('button', { name: ro.notificationPreferences.save }))

    expect(await screen.findByText(ro.notificationPreferences.saved)).toBeInTheDocument()
    expect(saved).toEqual([{ ...PREFERENCES, remind3Days: true }])
  })

  /**
   * The contradiction the developer saw on a real iPhone: notifications refused
   * in the phone's settings, and the account's switch still ticked on another
   * screen. On a phone that refuses, nothing set here could reach it, so nothing
   * is offered.
   */
  it('on a phone that refuses, offers no preferences at all', async () => {
    stubSignedIn()
    seam.push = phone('denied')

    renderApp(paths.notifications)

    expect(await screen.findByText(ro.notifications.denied)).toBeInTheDocument()
    expect(screen.queryByLabelText(ro.notificationPreferences.enabled)).not.toBeInTheDocument()
  })

  it('with reminders off, offers the switch and not the schedule nothing would be sent on', async () => {
    stubSignedIn({ remindersOn: false })

    renderApp(paths.notifications)

    expect(await screen.findByLabelText(ro.notificationPreferences.enabled)).not.toBeChecked()
    expect(screen.getByText(ro.notifications.paused)).toBeInTheDocument()
    expect(screen.queryByLabelText(ro.notificationPreferences.remind3Days)).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: ro.notificationPreferences.save })).not.toBeInTheDocument()
  })

  /**
   * Both saves start from what the server last said. A schedule edit that was
   * never saved is not smuggled out by the switch - the switch sends the
   * server's schedule with its own value changed.
   */
  it('the switch sends what the server holds, not a schedule edit that was never saved', async () => {
    const { saved } = stubSignedIn()

    renderApp(paths.notifications)
    await userEvent.click(await screen.findByLabelText(ro.notificationPreferences.remind3Days))
    await userEvent.click(screen.getByLabelText(ro.notificationPreferences.enabled))

    await waitFor(() => { expect(saved).toEqual([{ ...PREFERENCES, notificationsEnabled: false }]) })
  })
})
