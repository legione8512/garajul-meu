import type { i18n as I18n } from 'i18next'

/**
 * The application's one notification channel on Android, and the only one a
 * person sees under Settings, Apps, Garajul Meu, Notifications.
 *
 * <p><strong>Until version 1.0.2 (2026-09-22) there were two, and neither was
 * ours.</strong> A reminder arriving with the application open was posted by
 * @capacitor/local-notifications into the channel that plugin makes for itself,
 * named "Default" in English. One arriving with the application closed was
 * posted by Firebase into its own fallback channel, because no default had been
 * declared. So the same reminder landed in one of two places depending on
 * whether the application happened to be open, and switching one of them off
 * silenced half of the reminders without saying so.
 *
 * <p>The id is fixed for ever: Android keeps a channel's importance and sound as
 * the person last set them, and a new id would be a new channel with its
 * settings lost. `AndroidManifest.xml` names the same id as Firebase's default,
 * and `guards/notificationChannel.test.ts` holds the two together.
 */
export const REMINDER_CHANNEL_ID = 'reminders'

/**
 * The channels the two libraries make for themselves, deleted at every launch.
 *
 * <p>`default` is @capacitor/local-notifications' (8.3.1), and deleting it once
 * is not enough: `LocalNotificationsPlugin.load()` recreates it on every launch,
 * read in its source rather than assumed - `LocalNotificationManager.
 * createNotificationChannel()`, named "Default" in English. Its source also sets
 * `USAGE_ALARM` audio attributes, but only when `capacitor.config.ts` names a
 * sound of its own, which this application does not: on the Moto G6 Plus, on
 * 2026-09-22, the channel read `usage=USAGE_NOTIFICATION`. Claude had called it
 * an alarm channel from the function alone, without the condition above it.
 * Nothing is posted to it any more, because every notification names its channel.
 *
 * <p>`fcm_fallback_notification_channel` is Firebase's, made the first time a
 * background push arrives with no default channel declared - which was every
 * install before 1.0.2. With the manifest's default in place it is never made
 * again, so this only clears it from phones that already had it.
 *
 * <p>Deleting a channel also takes away anything still showing in it, so a
 * reminder left in the shade from before the update goes when the application
 * is next opened - once, on the first launch of 1.0.2.
 */
const GENERIC_CHANNELS = ['default', 'fcm_fallback_notification_channel'] as const

/**
 * Sound, and a place in the shade and the status bar, but no banner over what
 * the person is doing.
 *
 * <p><strong>No lock-screen visibility is set, because Android ignores it.</strong>
 * The first version asked for "private", and on the Moto G6 Plus (Android 9, on
 * 2026-09-22) the channel read `mLockscreenVisibility=-1000`, no override: an
 * application-created channel takes the application's setting, and only the
 * person can change it. What keeps a registration number off a secure lock
 * screen is each notification's own visibility, private by default - the
 * local-notifications plugin sets it explicitly and Firebase leaves Android's
 * default in place.
 */
const IMPORTANCE_DEFAULT = 3

/**
 * Creates the channel in the application's language, clears the libraries'
 * own, and renames it whenever the language changes - Android lets an
 * application change a channel's name and description, and nothing else, once it
 * exists.
 *
 * <p>Must finish before a notification can be posted to the channel: posting to
 * a channel that does not exist yet is dropped without a word. `main.tsx` starts
 * the foreground listener only after this.
 *
 * <p><strong>Android 7 has no channels</strong> - the tablet the application
 * declares as its minimum runs 7.0 - and the plugin answers "unavailable" there.
 * That is not a failure: a notification simply has no channel below Android 8.
 *
 * <p>The plugin is imported inside the function and never returned - see
 * `backButton.ts` for why.
 */
export async function watchReminderChannel(i18n: I18n): Promise<void> {
  const { Capacitor } = await import('@capacitor/core')

  if (Capacitor.getPlatform() !== 'android') {
    return
  }

  const { LocalNotifications } = await import('@capacitor/local-notifications')

  const createInCurrentLanguage = () => LocalNotifications.createChannel({
    id: REMINDER_CHANNEL_ID,
    name: i18n.t('reminders.channel.name'),
    description: i18n.t('reminders.channel.description'),
    importance: IMPORTANCE_DEFAULT,
    vibration: true,
  })

  try {
    await createInCurrentLanguage()

    for (const id of GENERIC_CHANNELS) {
      await LocalNotifications.deleteChannel({ id })
    }
  }
  catch {
    // Android 7, where there are no channels to make or to delete.
    return
  }

  i18n.on('languageChanged', () => {
    createInCurrentLanguage().catch(() => {
      // The channel keeps its previous name, which is the whole of the harm.
    })
  })
}
