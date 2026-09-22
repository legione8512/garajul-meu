import i18next, { type i18n as I18n } from 'i18next'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { en } from '../i18n/locales/en.ts'
import { ro } from '../i18n/locales/ro.ts'

interface ChannelOptions {
  id: string
  name: string
  description?: string
  importance?: number
  visibility?: number
  vibration?: boolean
}

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

const local = vi.hoisted(() => ({
  createChannel: vi.fn<(channel: ChannelOptions) => Promise<void>>(),
  deleteChannel: vi.fn<(args: { id: string }) => Promise<void>>(),
  then: () => { trap() },
}))

const platform = vi.hoisted(() => ({ current: 'android' }))

vi.mock('@capacitor/local-notifications', () => ({ LocalNotifications: local }))
vi.mock('@capacitor/core', () => ({ Capacitor: { getPlatform: () => platform.current } }))

const { REMINDER_CHANNEL_ID, watchReminderChannel } = await import('./reminderChannel.ts')

/** A real i18next with the application's own wording, so a missing key fails here. */
async function i18nIn(language: 'ro' | 'en'): Promise<I18n> {
  const instance = i18next.createInstance()
  await instance.init({
    resources: { ro: { translation: ro }, en: { translation: en } },
    lng: language,
    fallbackLng: 'ro',
  })
  return instance
}

describe('the reminder channel', () => {
  beforeEach(() => {
    // `restoreMocks` restores spies, not a bare `vi.fn()` from `vi.hoisted`.
    local.createChannel.mockReset()
    local.createChannel.mockResolvedValue(undefined)
    local.deleteChannel.mockReset()
    local.deleteChannel.mockResolvedValue(undefined)
    platform.current = 'android'
  })

  /**
   * Android keys a person's choices - importance, sound, whether it is on at
   * all - to the id. A different id would be a second channel with those
   * choices lost, and the manifest names this one too.
   */
  it('keeps the id it was released with', () => {
    expect(REMINDER_CHANNEL_ID).toBe('reminders')
  })

  /**
   * Heard, but not thrown over whatever the person is doing. The importance
   * and the sound are fixed when the channel is first made - Android does not
   * let an application change them afterwards - so this is the one chance to
   * get them right. No lock-screen visibility: Android ignores it from an
   * application, as the Moto G6 Plus showed on 2026-09-22.
   */
  it('is made in the application\'s language, heard, and vibrating', async () => {
    await watchReminderChannel(await i18nIn('ro'))

    expect(local.createChannel).toHaveBeenCalledWith({
      id: 'reminders',
      name: 'Memento-uri',
      description: ro.reminders.channel.description,
      importance: 3,
      vibration: true,
    })
  })

  /**
   * The two channels that split one reminder in two before 1.0.2. The plugin
   * recreates its own at every launch, so they go at every launch - and only
   * once the application's own exists, so a reminder always has somewhere to go.
   */
  it('deletes the channels the libraries make for themselves, after making its own', async () => {
    await watchReminderChannel(await i18nIn('ro'))

    expect(local.deleteChannel.mock.calls.map(([args]) => args.id))
      .toEqual(['default', 'fcm_fallback_notification_channel'])
    expect(local.createChannel.mock.invocationCallOrder[0])
      .toBeLessThan(local.deleteChannel.mock.invocationCallOrder[0] ?? 0)
  })

  it('follows the application into another language, under the same id', async () => {
    const i18n = await i18nIn('ro')
    await watchReminderChannel(i18n)

    await i18n.changeLanguage('en')

    expect(local.createChannel).toHaveBeenLastCalledWith(expect.objectContaining({
      id: 'reminders',
      name: 'Reminders',
      description: en.reminders.channel.description,
    }))
  })

  /**
   * The tablet the application declares as its minimum runs Android 7.0,
   * which has no channels: the plugin answers "unavailable". Nothing is
   * wrong, and nothing more is attempted.
   */
  it('accepts that Android 7 has no channels', async () => {
    local.createChannel.mockRejectedValue(new Error('unavailable'))
    const i18n = await i18nIn('ro')

    await expect(watchReminderChannel(i18n)).resolves.toBeUndefined()
    await i18n.changeLanguage('en')

    expect(local.deleteChannel).not.toHaveBeenCalled()
    expect(local.createChannel).toHaveBeenCalledOnce()
  })

  it('does nothing on iOS, which has no channels either', async () => {
    platform.current = 'ios'

    await watchReminderChannel(await i18nIn('ro'))

    expect(local.createChannel).not.toHaveBeenCalled()
    expect(local.deleteChannel).not.toHaveBeenCalled()
  })
})
