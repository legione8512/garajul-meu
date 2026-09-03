import { beforeEach, describe, expect, it, vi } from 'vitest'

import type { Push } from './push.ts'

const registerDevice = vi.hoisted(() => vi.fn())
const seam = vi.hoisted(() => ({ push: null as Push | null }))

vi.mock('../api/endpoints/devices.ts', () => ({ registerDevice }))
vi.mock('./push.ts', () => seam)

const { reportDevice } = await import('./reportDevice.ts')

function phone(permission: 'prompt' | 'granted' | 'denied'): Push {
  return {
    permission: () => Promise.resolve(permission),
    request: () => Promise.resolve(permission),
    token: () => Promise.resolve('an-fcm-token'),
    platform: () => Promise.resolve('ANDROID'),
  }
}

/**
 * Three rules, and each one exists to stop a different wrong thing.
 */
describe('reporting the device at launch', () => {
  beforeEach(() => {
    registerDevice.mockResolvedValue({})
    seam.push = null
  })

  it('does nothing at all in a browser, where there is no push to report', async () => {
    await reportDevice()

    expect(registerDevice).not.toHaveBeenCalled()
  })

  /**
   * The privacy rule. Registering would mint an FCM token - a durable
   * identifier for this handset - and send it before the person has been asked
   * anything at all.
   */
  it('creates no token while the question has not been put', async () => {
    seam.push = phone('prompt')

    await reportDevice()

    expect(registerDevice).not.toHaveBeenCalled()
  })

  it('registers with the truth once permission has been given', async () => {
    seam.push = phone('granted')

    await reportDevice()

    expect(registerDevice).toHaveBeenCalledWith({
      platform: 'ANDROID',
      pushToken: 'an-fcm-token',
      notificationsEnabled: true,
    })
  })

  /**
   * The one that looks wrong and is not. A refusal still registers, because
   * POST_NOTIFICATIONS governs whether a notification may be *shown*, not
   * whether the application may register with Firebase - so the token is real
   * and works, and only `notificationsEnabled` says what it cannot do.
   *
   * <p>Without this the server keeps a device it believes can receive, Firebase
   * accepts every message for it, and the dispatcher records reminders as sent
   * that nobody could ever have seen.
   */
  it('registers a refusal too, which is the whole reason the flag exists', async () => {
    seam.push = phone('denied')

    await reportDevice()

    expect(registerDevice).toHaveBeenCalledWith({
      platform: 'ANDROID',
      pushToken: 'an-fcm-token',
      notificationsEnabled: false,
    })
  })
})