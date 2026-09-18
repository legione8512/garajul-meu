import type { Push, PushPermission } from './push.ts'

/**
 * Firebase Cloud Messaging, and the plugin was changed on 2026-09-05 for one
 * reason: token type.
 *
 * <p>`@capacitor/push-notifications` returns an **APNs** token on iOS and an FCM
 * token on Android - its own type definitions say so. The backend speaks only
 * FCM, so every iOS device would have registered a token Firebase cannot
 * address. **And the failure would have been silent**: FCM answers
 * `INVALID_ARGUMENT`, which `FirebasePushNotificationProvider` deliberately
 * treats as transient so a payload bug cannot unregister working phones. Each
 * reminder would have been retried its few times and dropped, for ever, with the
 * device looking healthy in the database.
 *
 * <p>This plugin returns an FCM token on both platforms, so one backend path
 * serves both and nothing on the server changes.
 *
 * <p>The listener machinery the old plugin needed is gone with it: `getToken()`
 * is a promise. <strong>It is not a promise that always settles</strong>: on iOS
 * it waits for an APNs registration, and a failed one reaches nothing
 * (`63db063`), so the bound lives in `reportDevice`, the one caller of `token()`.
 * Nor is it one that succeeds the first time on a fresh installation: asked
 * before APNs has answered, Firebase refuses at once, which is why
 * `reportDevice` asks again inside that same bound (2026-09-14).
 *
 * <p><strong>On Android the permission is not asked of this plugin at all</strong>
 * (2026-09-18) - see `permission()` below.
 */

/** See `PushPermission`: two of Capacitor's four states mean the same thing here. */
function simplified(receive: string): PushPermission {
  if (receive === 'granted') {
    return 'granted'
  }
  return receive === 'denied' ? 'denied' : 'prompt'
}

/** Which platform this build is running on, asked of it rather than assumed. */
async function onAndroid(): Promise<boolean> {
  const { Capacitor } = await import('@capacitor/core')
  return Capacitor.getPlatform() === 'android'
}

/**
 * Imported inside each method and never handed onward - a Capacitor plugin is a
 * Proxy that looks thenable, and returning one from an `async` function makes
 * the promise never settle. That cost the first iOS run on 2026-09-04;
 * `keystoreSecureStore.ts` carries the full account.
 */
export const nativePush: Push = {
  /**
   * What the operating system allows - and on Android that question goes to
   * `@capacitor/local-notifications`, because the messaging plugin does not ask
   * it below Android 13.
   *
   * <p><strong>Seen on 2026-09-18, on a Moto G6 Plus running Android 9.</strong>
   * Notifications switched off in the application's own Android settings;
   * screen 18 still said they were active, and the registration it sent carried
   * `notificationsEnabled: true`. `FirebaseMessagingPlugin.java` answers
   * `granted` **unconditionally below API 33** (line 94) and never calls
   * `areNotificationsEnabled()`. So the server went on believing it could
   * deliver, `ReminderDispatcher` recorded every reminder as SENT, and nothing was
   * shown - the lie `reportDevice` exists to prevent, arriving by a road it did
   * not cover. It also made a sentence of the published privacy policy false:
   * push is sent "numai după ce le permiți în sistemul dispozitivului", and
   * withdrawing that in settings changed nothing.
   *
   * <p>`LocalNotifications.checkPermissions()` below API 33 answers from
   * `NotificationManagerCompat.areNotificationsEnabled()` - read in
   * `LocalNotificationsPlugin.kt` (8.3.1), lines 463 and 516, before choosing
   * it. From API 33 both plugins read the same runtime permission,
   * `POST_NOTIFICATIONS`, so asking this one on every Android version changes
   * nothing there and keeps one source for the answer.
   *
   * <p><strong>The consent this reports is the system's</strong>, which is what the
   * privacy policy promises. Below Android 13 the system allows notifications
   * until the person turns them off, so a signed-in phone registers without
   * being asked anything - decided with the developer on 2026-09-18 as the
   * policy's own model, not an oversight. What the fix adds is that turning them
   * off is now heard.
   *
   * <p>iOS keeps the messaging plugin, whose answer there comes from
   * `UNUserNotificationCenter` and is true. It is also frozen at 1.0.1 until a
   * Mac is available, and has no build carrying the new plugin yet.
   */
  async permission() {
    if (await onAndroid()) {
      const { LocalNotifications } = await import('@capacitor/local-notifications')
      return simplified((await LocalNotifications.checkPermissions()).display)
    }

    const { FirebaseMessaging } = await import('@capacitor-firebase/messaging')
    return simplified((await FirebaseMessaging.checkPermissions()).receive)
  },

  /**
   * Raises the system dialog where one exists, for the same reason and from the
   * same plugin as `permission()`. Below Android 13 there is no dialog to raise,
   * and the answer is simply the state the person left their settings in - so a
   * phone with notifications switched off reads `denied`, and screen 18 says
   * where to change it instead of pretending it has done so.
   */
  async request() {
    if (await onAndroid()) {
      const { LocalNotifications } = await import('@capacitor/local-notifications')
      return simplified((await LocalNotifications.requestPermissions()).display)
    }

    const { FirebaseMessaging } = await import('@capacitor-firebase/messaging')
    return simplified((await FirebaseMessaging.requestPermissions()).receive)
  },

  /**
   * <p><strong>Calling this is what creates the token, and that is deliberate.</strong>
   * FCM auto-initialisation is switched off in both the Android manifest and
   * `Info.plist`, so the library generates nothing and uploads nothing to
   * Firebase until this runs - and the plugin documents that `getToken` re-enables
   * auto-init, which is right, because by then somebody has been asked.
   *
   * <p>No options are passed: the only ones that exist are `vapidKey` and a
   * service worker registration, both web-only, and this file never runs there.
   */
  async token() {
    const { FirebaseMessaging } = await import('@capacitor-firebase/messaging')
    return (await FirebaseMessaging.getToken()).token
  },

  async platform() {
    const { Capacitor } = await import('@capacitor/core')
    return Capacitor.getPlatform() === 'ios' ? 'IOS' : 'ANDROID'
  },
}
