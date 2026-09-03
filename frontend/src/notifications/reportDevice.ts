import { registerDevice } from '../api/endpoints/devices.ts'

import { push } from './push.ts'

/**
 * Tells the account what this installation can currently do, on every launch.
 *
 * <p>`DeviceController.register` is an upsert answering 200 rather than 201 for
 * exactly this: the usual answer is "the registration you already had". What
 * changes between launches is `notificationsEnabled`, and it is the reason this
 * runs every time instead of once.
 *
 * <p><strong>The lie it prevents.</strong> A permission granted in March and
 * revoked in June leaves the token perfectly valid. Firebase accepts the
 * message, `ReminderDispatcher` records the reminder as SENT, and the person is
 * shown nothing at all - the system reports success for a notification nobody
 * could ever have seen. Reporting the truth on every launch is what keeps the
 * delivery record honest, and it costs no extra request because the endpoint is
 * called anyway.
 *
 * <p><strong>Nothing happens while the answer is still `prompt`, and that is a
 * privacy decision rather than an optimisation.</strong> Registering would call
 * `register()`, which mints an FCM token - a durable identifier for this
 * handset - and send it to the server before the person has ever been asked
 * anything. Screen 18 is where the asking happens; until it has, there is
 * nothing to report and nothing worth creating.
 *
 * <p><strong>A refusal is still registered, and the token still fetched.</strong>
 * That looks contradictory and is not: on Android 13 and later
 * `POST_NOTIFICATIONS` governs whether a notification may be *displayed*, not
 * whether the application may register with FCM. The token exists and works; it
 * simply cannot show anything. Sending it with `notificationsEnabled: false` is
 * precisely the honest report the column was added for.
 */
export async function reportDevice(): Promise<void> {
  const device = push

  if (device === null) {
    return
  }

  const permission = await device.permission()

  if (permission === 'prompt') {
    return
  }

  await registerDevice({
    platform: await device.platform(),
    pushToken: await device.token(),
    notificationsEnabled: permission === 'granted',
  })
}