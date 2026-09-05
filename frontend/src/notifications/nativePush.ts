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
 * <p>The listener-and-timeout machinery the old plugin needed is gone with it:
 * `getToken()` is a promise.
 */

/** See `PushPermission`: two of Capacitor's four states mean the same thing here. */
function simplified(receive: string): PushPermission {
  if (receive === 'granted') {
    return 'granted'
  }
  return receive === 'denied' ? 'denied' : 'prompt'
}

/**
 * Imported inside each method and never handed onward - a Capacitor plugin is a
 * Proxy that looks thenable, and returning one from an `async` function makes
 * the promise never settle. That cost the first iOS run on 2026-09-04;
 * `keystoreSecureStore.ts` carries the full account.
 */
export const nativePush: Push = {
  async permission() {
    const { FirebaseMessaging } = await import('@capacitor-firebase/messaging')
    return simplified((await FirebaseMessaging.checkPermissions()).receive)
  },

  async request() {
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