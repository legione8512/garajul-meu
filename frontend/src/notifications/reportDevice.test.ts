import { beforeEach, describe, expect, it, vi } from 'vitest'

import type { Push } from './push.ts'

const registerDevice = vi.hoisted(() => vi.fn())
const seam = vi.hoisted(() => ({ push: null as Push | null }))

vi.mock('../api/endpoints/devices.ts', () => ({ registerDevice }))
vi.mock('./push.ts', () => seam)

const { reportDevice } = await import('./reportDevice.ts')

/**
 * Spelled out a second time rather than exported from the module, because a test
 * that imported the constant would agree with any value it was changed to. This
 * string is a promise to every installation that already stored something under
 * it: change it and every phone forgets the token it registered, so the day a
 * permission is revoked there is nothing to correct. That is worth a test that
 * fails on the rename.
 */
const REGISTERED_TOKEN = 'garajul-meu.push-token'

/**
 * A handset whose `token()` is observable, because half of what these rules say
 * is about *not* calling it.
 */
function phone(
  permission: 'prompt' | 'granted' | 'denied',
  { token = 'an-fcm-token', platform = 'ANDROID' }: {
    token?: string
    platform?: 'ANDROID' | 'IOS'
  } = {},
) {
  return {
    permission: vi.fn(() => Promise.resolve(permission)),
    request: vi.fn(() => Promise.resolve(permission)),
    token: vi.fn(() => Promise.resolve(token)),
    platform: vi.fn(() => Promise.resolve(platform)),
  }
}

/**
 * The rules changed on 2026-09-05, when the plugin swap made the old refusal
 * rule wrong on iOS - and the test that guarded it failed, which is what it was
 * for.
 */
describe('reporting the device at launch', () => {
  beforeEach(() => {
    // `restoreMocks` restores spies, not a bare `vi.fn()` from `vi.hoisted`, so
    // its call history would otherwise carry into the next test - and half the
    // assertions here are `not.toHaveBeenCalled`, which such a leak turns green
    // to red for the wrong reason.
    registerDevice.mockReset()
    registerDevice.mockResolvedValue({})
    seam.push = null
  })

  it('does nothing at all in a browser, where there is no push to report', async () => {
    await reportDevice()

    expect(registerDevice).not.toHaveBeenCalled()
  })

  /**
   * The privacy rule. With FCM auto-initialisation off in both native projects
   * no token exists until `getToken` is called, so asking here would mint a
   * durable identifier for this handset and upload it to Firebase before the
   * person had been asked anything at all.
   */
  it('mints no token while the question has not been put', async () => {
    const device = phone('prompt')
    seam.push = device

    await reportDevice()

    expect(device.token).not.toHaveBeenCalled()
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

  it('remembers what it registered, which is the only way a later refusal has anything to say', async () => {
    seam.push = phone('granted')

    await reportDevice()

    expect(localStorage.getItem(REGISTERED_TOKEN)).toBe('an-fcm-token')
  })

  /**
   * The case the flag exists for: granted in March, switched off in June. The
   * token stays perfectly valid, so without this launch's correction Firebase
   * accepts every message, `ReminderDispatcher` records the reminder as SENT,
   * and the person is shown nothing.
   *
   * <p>Written as two launches rather than a seeded key because the two halves
   * are one mechanism - the first launch is what makes the second one possible,
   * and a seeded key would pass even if `remember` had been deleted.
   */
  it('corrects a registration it made, when the permission is later withdrawn', async () => {
    seam.push = phone('granted', { token: 'the-token-from-march' })
    await reportDevice()
    registerDevice.mockClear()

    const june = phone('denied', { platform: 'IOS' })
    seam.push = june

    await reportDevice()

    expect(june.token).not.toHaveBeenCalled()
    expect(registerDevice).toHaveBeenCalledWith({
      platform: 'IOS',
      pushToken: 'the-token-from-march',
      notificationsEnabled: false,
    })
  })

  /**
   * Somebody who declines at the very first prompt has no registration to
   * correct, and leaves no trace. The old rule minted a token here on the
   * argument that Android's POST_NOTIFICATIONS governs display rather than
   * registration - true on Android, false on iOS, and in both cases the wrong
   * thing to do to somebody who has just said no.
   */
  it('leaves no trace of somebody who refused before ever registering', async () => {
    const device = phone('denied')
    seam.push = device

    await reportDevice()

    expect(device.token).not.toHaveBeenCalled()
    expect(registerDevice).not.toHaveBeenCalled()
  })

  /**
   * Private browsing and blocked site data make `localStorage` throw rather than
   * answer empty. A launch is not the place to discover that: the registration
   * is the point, and losing the memory of it only costs the later correction.
   */
  it('still registers when storage refuses to remember', async () => {
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new DOMException('The quota has been exceeded.', 'QuotaExceededError')
    })
    seam.push = phone('granted')

    await reportDevice()

    expect(registerDevice).toHaveBeenCalledWith({
      platform: 'ANDROID',
      pushToken: 'an-fcm-token',
      notificationsEnabled: true,
    })
  })

  it('reports no refusal when storage refuses to answer', async () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new DOMException('The operation is insecure.', 'SecurityError')
    })
    seam.push = phone('denied')

    await reportDevice()

    expect(registerDevice).not.toHaveBeenCalled()
  })
})
