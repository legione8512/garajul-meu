import { beforeEach, describe, expect, it, vi } from 'vitest'

interface Received {
  notification: { id?: string, title?: string, body?: string }
}

interface Posted {
  notifications: { id: number, title: string, body: string, isExactNotification?: boolean }[]
}

type Listener = (event: Received) => void

/**
 * The `then` trap every native seam's test carries - see
 * `keystoreSecureStore.test.ts` for the hang it turns into a red test.
 */
function trap(): never {
  throw new Error(
    'The plugin object was used as the resolution value of a promise. A Capacitor '
    + 'proxy is thenable-looking, so this hangs for ever on a device: import it '
    + 'inside the function that uses it and never return it.',
  )
}

const firebase = vi.hoisted(() => ({
  addListener: vi.fn<(event: string, listener: Listener) => Promise<{ remove: () => Promise<void> }>>(),
  then: () => { trap() },
}))

const local = vi.hoisted(() => ({
  schedule: vi.fn<(options: Posted) => Promise<unknown>>(),
  then: () => { trap() },
}))

const platform = vi.hoisted(() => ({ current: 'android' }))

vi.mock('@capacitor-firebase/messaging', () => ({ FirebaseMessaging: firebase }))
vi.mock('@capacitor/local-notifications', () => ({ LocalNotifications: local }))
vi.mock('@capacitor/core', () => ({ Capacitor: { getPlatform: () => platform.current } }))

const { notificationId, showForegroundNotifications } = await import('./foregroundNotifications.ts')

/** Starts the module and hands back its listener, so a test can deliver a push. */
async function deliver(): Promise<Listener> {
  await showForegroundNotifications()

  const call = firebase.addListener.mock.calls.at(0)

  if (call === undefined) {
    throw new Error('showForegroundNotifications registered no listener')
  }

  const [event, listener] = call
  expect(event).toBe('notificationReceived')
  return listener
}

/** The one notification the last `schedule()` call posted. */
function posted(): Posted['notifications'][number] {
  const notification = local.schedule.mock.calls.at(-1)?.[0].notifications[0]

  if (notification === undefined) {
    throw new Error('nothing was posted')
  }

  return notification
}

const REMINDER = {
  id: '0:1789746134106828%13921e3c13921e3c',
  title: 'ITP expiră mâine',
  body: 'Dacia Logan, B 00 XYZ',
}

describe('a push arriving while the application is open', () => {
  beforeEach(() => {
    // `restoreMocks` restores spies, not a bare `vi.fn()` from `vi.hoisted`.
    firebase.addListener.mockReset()
    firebase.addListener.mockResolvedValue({ remove: () => Promise.resolve() })
    local.schedule.mockReset()
    local.schedule.mockResolvedValue({ notifications: [] })
    platform.current = 'android'
  })

  /** What was seen on a Moto G6 Plus: delivered to the application, shown by nobody. */
  it('is shown on Android, with its own title and text', async () => {
    const push = await deliver()

    push({ notification: REMINDER })

    expect(posted()).toMatchObject({ title: REMINDER.title, body: REMINDER.body })
  })

  /**
   * The line that cannot be lost. `schedule()` wants an exact alarm by default,
   * and on Android 12 and later, with the permission the manifest removes, it
   * would open the Alarms & reminders settings screen before posting anything.
   * The phone this was tested on runs Android 9, below that check - so only this
   * assertion stands between the change and every newer phone.
   */
  it('never asks for an exact alarm', async () => {
    const push = await deliver()

    push({ notification: REMINDER })

    expect(posted().isExactNotification).toBe(false)
  })

  /** FCM may deliver a message twice; the second should replace the first, not join it. */
  it('gives the same message the same identifier, and different messages different ones', async () => {
    const push = await deliver()

    push({ notification: REMINDER })
    const first = posted().id
    push({ notification: REMINDER })
    const again = posted().id
    push({ notification: { ...REMINDER, id: '0:1789746134106829%13921e3c13921e3c' } })
    const other = posted().id

    expect(again).toBe(first)
    expect(other).not.toBe(first)
  })

  /**
   * A local notification's identifier is a Java `int`, and a negative one is
   * asking for trouble. `polygenelubricants` is here on purpose: its hash is
   * exactly `Integer.MIN_VALUE`, the one value `Math.abs` leaves negative, which
   * is why the identifier is masked rather than made absolute.
   */
  it('keeps every identifier a positive 32-bit integer', () => {
    for (const messageId of [REMINDER.id, 'polygenelubricants', '', 'x', 'a'.repeat(4096), '0:9999999999999999%ffffffffffffffff']) {
      const id = notificationId(messageId)

      expect(Number.isInteger(id)).toBe(true)
      expect(id).toBeGreaterThanOrEqual(0)
      expect(id).toBeLessThanOrEqual(0x7fffffff)
    }
  })

  it('posts nothing for a message with nothing to show', async () => {
    const push = await deliver()

    push({ notification: { id: REMINDER.id } })

    expect(local.schedule).not.toHaveBeenCalled()
  })

  /**
   * The person switched notifications off, and a reminder already on its way
   * still arrived. The plugin refuses, and refusing is correct - so the refusal
   * must end here rather than surface as an unhandled rejection.
   */
  it('accepts being refused when notifications are switched off', async () => {
    local.schedule.mockRejectedValue(new Error('Notifications are not enabled'))
    const push = await deliver()

    push({ notification: REMINDER })
    await new Promise((resolve) => { setTimeout(resolve, 0) })

    expect(local.schedule).toHaveBeenCalledOnce()
  })

  /** iOS presents foreground pushes itself; doing it here too would show each one twice. */
  it('does nothing on iOS, which already shows them', async () => {
    platform.current = 'ios'

    await showForegroundNotifications()

    expect(firebase.addListener).not.toHaveBeenCalled()
  })
})
