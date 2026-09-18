import { beforeEach, describe, expect, it, vi } from 'vitest'

/**
 * The `then` trap every native seam's test carries - see
 * `keystoreSecureStore.test.ts` for the hang it turns into a red test.
 */
function trap(): never {
  throw new Error(
    'The plugin object was used as the resolution value of a promise. A Capacitor '
    + 'proxy is thenable-looking, so this hangs for ever on a device: import it '
    + 'inside the method that uses it and never return it.',
  )
}

type Answer = Promise<{ display: string }>
type FirebaseAnswer = Promise<{ receive: string }>

const local = vi.hoisted(() => ({
  checkPermissions: vi.fn<() => Answer>(),
  requestPermissions: vi.fn<() => Answer>(),
  then: () => { trap() },
}))

const firebase = vi.hoisted(() => ({
  checkPermissions: vi.fn<() => FirebaseAnswer>(),
  requestPermissions: vi.fn<() => FirebaseAnswer>(),
  then: () => { trap() },
}))

const platform = vi.hoisted(() => ({ current: 'android' }))

vi.mock('@capacitor/local-notifications', () => ({ LocalNotifications: local }))
vi.mock('@capacitor-firebase/messaging', () => ({ FirebaseMessaging: firebase }))
vi.mock('@capacitor/core', () => ({ Capacitor: { getPlatform: () => platform.current } }))

const { nativePush } = await import('./nativePush.ts')

/**
 * Every test below sets the two plugins to disagree, because agreement proves
 * nothing about which one was asked. Firebase says `granted` in all of them -
 * which is what it says, unconditionally, on every Android phone before 13.
 */
describe('asking the platform whether notifications may be shown', () => {
  beforeEach(() => {
    // `restoreMocks` restores spies, not a bare `vi.fn()` from `vi.hoisted`.
    for (const fn of [local.checkPermissions, local.requestPermissions, firebase.checkPermissions, firebase.requestPermissions]) {
      fn.mockReset()
    }
    firebase.checkPermissions.mockResolvedValue({ receive: 'granted' })
    firebase.requestPermissions.mockResolvedValue({ receive: 'granted' })
    platform.current = 'android'
  })

  /**
   * The defect reproduced on a Moto G6 Plus on 2026-09-18: notifications off in
   * the phone's settings, and the application still reporting them on.
   */
  it('hears notifications switched off in Android settings, which Firebase does not', async () => {
    local.checkPermissions.mockResolvedValue({ display: 'denied' })

    await expect(nativePush.permission()).resolves.toBe('denied')
    expect(firebase.checkPermissions).not.toHaveBeenCalled()
  })

  /**
   * Below Android 13 there is no dialog to raise, so a request is only a reading
   * of the settings - and asking Firebase would have answered `granted` again.
   */
  it('answers a request on Android from the same source', async () => {
    local.requestPermissions.mockResolvedValue({ display: 'denied' })

    await expect(nativePush.request()).resolves.toBe('denied')
    expect(firebase.requestPermissions).not.toHaveBeenCalled()
  })

  /** Android 13's dialog asked once and not yet answered is still the question not put. */
  it('reads a rationale still owed as the question not yet put', async () => {
    local.checkPermissions.mockResolvedValue({ display: 'prompt-with-rationale' })

    await expect(nativePush.permission()).resolves.toBe('prompt')
  })

  /**
   * iOS is untouched: its answer from the messaging plugin is true there, and no
   * iOS build carries the new plugin.
   */
  it('keeps asking the messaging plugin on iOS', async () => {
    platform.current = 'ios'
    firebase.checkPermissions.mockResolvedValue({ receive: 'denied' })
    firebase.requestPermissions.mockResolvedValue({ receive: 'denied' })

    await expect(nativePush.permission()).resolves.toBe('denied')
    await expect(nativePush.request()).resolves.toBe('denied')
    expect(local.checkPermissions).not.toHaveBeenCalled()
    expect(local.requestPermissions).not.toHaveBeenCalled()
  })
})
