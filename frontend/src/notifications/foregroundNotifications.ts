/**
 * Shows a push that arrives while the application is open, on Android.
 *
 * <p><strong>Seen on 2026-09-18, on a Moto G6 Plus running Android 9.</strong> A
 * test message from the Firebase console, sent with the application on screen,
 * reached it - a `notificationReceived` listener received its title and body -
 * and nothing appeared on the phone. With the application in the background the
 * same message sounded and sat in the shade. That is how Android treats a
 * notification message in the foreground: it hands it to the application and
 * shows it to nobody, and the messaging plugin forwards it and stops there. A
 * reminder that a document expires tomorrow is not less worth seeing because
 * the application happened to be open.
 *
 * <p>So it is posted again, straight away, as a local notification - the answer
 * `capacitor.config.ts` has named beside `presentationOptions` since 2026-09-11.
 *
 * <p><strong>`isExactNotification: false` is not optional.</strong> Read in
 * `LocalNotificationsPlugin.kt` (8.3.1) before writing this: `schedule()` asks
 * whether any notification wants an exact alarm - by default yes, whether or not
 * it has a schedule at all - and on Android 12 and later, without the exact-alarm
 * permission, it opens the *Alarms & reminders* settings screen before posting
 * anything. The manifest removes that permission. Without this line every push
 * arriving with the application open would have thrown the person into
 * settings - on every phone except the Android 9 one this was tested on, which
 * is below the version where the check applies, so the device could not have
 * shown it. With no schedule, the plugin goes straight to
 * `NotificationManager.notify()`.
 *
 * <p><strong>Android only.</strong> iOS already presents a push in the
 * foreground, by `presentationOptions`, so doing this there would show every
 * reminder twice.
 *
 * <p>Registering at launch loads the messaging plugin before anybody has signed
 * in, and that was checked rather than assumed: on Android its `load()` builds
 * the implementation and replays anything cached, and no token exists until
 * `getToken()` runs. FCM auto-initialisation stays off.
 *
 * <p>Both plugins are imported inside the function and never returned - see
 * `backButton.ts` for why.
 */

/** Java's `int` range, which a local notification's identifier must fit. */
const POSITIVE_INT = 0x7fffffff

/**
 * A positive 32-bit identifier, always the same for the same message.
 *
 * <p>Derived from the message's own FCM id rather than counted, so the same
 * message delivered twice - which FCM permits - replaces its notification
 * instead of stacking a second one beside it.
 */
export function notificationId(messageId: string): number {
  let hash = 0

  for (let index = 0; index < messageId.length; index++) {
    hash = (Math.imul(31, hash) + messageId.charCodeAt(index)) | 0
  }

  return hash & POSITIVE_INT
}

export async function showForegroundNotifications(): Promise<void> {
  const { Capacitor } = await import('@capacitor/core')

  if (Capacitor.getPlatform() !== 'android') {
    return
  }

  const { FirebaseMessaging } = await import('@capacitor-firebase/messaging')
  const { LocalNotifications } = await import('@capacitor/local-notifications')

  await FirebaseMessaging.addListener('notificationReceived', ({ notification }) => {
    const { id, title, body } = notification

    // A data-only message carries nothing to show. The backend sends none today;
    // posting an empty notification would be worse than posting nothing.
    if (title === undefined && body === undefined) {
      return
    }

    LocalNotifications.schedule({
      notifications: [{
        id: id === undefined ? Date.now() & POSITIVE_INT : notificationId(id),
        title: title ?? '',
        body: body ?? '',
        isExactNotification: false,
      }],
    }).catch(() => {
      // The plugin refuses when notifications are switched off, and that refusal
      // is the right outcome rather than a failure: the person turned them off,
      // so nothing is shown. It happens between their switching off and the next
      // launch reporting it, while reminders already on their way still arrive.
    })
  })
}
