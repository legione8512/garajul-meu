import { registerDevice } from '../api/endpoints/devices.ts'

import { push } from './push.ts'

/**
 * The token this installation last registered, so a refusal can be reported
 * without minting anything.
 *
 * <p>Not a secret - the server already has it, and it travels in every
 * registration - so ordinary storage rather than the Keychain, which is for the
 * refresh credential and should stay uncrowded.
 */
const REGISTERED_TOKEN = 'garajul-meu.push-token'

function remembered(): string | null {
  try {
    return localStorage.getItem(REGISTERED_TOKEN)
  }
  catch {
    // Private browsing, blocked site data. Having no memory of a registration is
    // an ordinary state and the rules below all have an answer for it.
    return null
  }
}

function remember(token: string): void {
  try {
    localStorage.setItem(REGISTERED_TOKEN, token)
  }
  catch {
    // The registration still happened; only the ability to report a later
    // revocation is lost. See the trigger in the notes below.
  }
}

/**
 * Tells the account what this installation can currently do, on every launch.
 *
 * <p>`DeviceController.register` is an upsert answering 200 rather than 201 for
 * exactly this: the usual answer is "the registration you already had". What
 * changes between launches is `notificationsEnabled`, which is why this runs
 * every time instead of once.
 *
 * <p><strong>The lie it prevents.</strong> A permission granted in March and
 * revoked in June leaves the token perfectly valid. Firebase accepts the
 * message, `ReminderDispatcher` records the reminder as SENT, and the person is
 * shown nothing at all. Reporting the truth on every launch is what keeps the
 * delivery record honest.
 *
 * <p><strong>Three rules, and the middle one changed on 2026-09-05.</strong>
 *
 * <p><em>Never asked.</em> Nothing happens, and that is privacy rather than
 * economy: FCM auto-initialisation is off in both native projects, so no token
 * exists until `getToken` is called, and calling it before anybody has been
 * asked would mint a durable identifier for this handset and upload it to
 * Firebase. Screen 18 is where the asking happens.
 *
 * <p><em>Granted.</em> Ask for the token, remember it, register it as able.
 *
 * <p><em>Refused.</em> Report the *remembered* token as unable, and mint nothing.
 * The old rule asked for a token here too, on the argument that Android's
 * POST_NOTIFICATIONS governs display rather than registration - true there, and
 * false on iOS, where the FCM token comes through an APNs registration that
 * needs the permission. With auto-init off it is also the wrong thing to do to
 * somebody who has just declined: they refused notifications, not
 * identification. So a refusal is reported only when there is already a
 * registration to correct, which is the case that matters - a revocation.
 * Somebody who declines at the first prompt and never grants leaves no trace,
 * which is exactly right.
 *
 * <p>TRIGGER for revisiting: a device whose notifications were switched off in
 * settings still showing as able. That would mean the remembered token was lost
 * - cleared site data, a reinstall - and the revocation had nothing to report
 * against.
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

  if (permission === 'denied') {
    const previous = remembered()

    if (previous !== null) {
      await registerDevice({
        platform: await device.platform(),
        pushToken: previous,
        notificationsEnabled: false,
      })
    }

    return
  }

  const token = await device.token()
  remember(token)

  await registerDevice({
    platform: await device.platform(),
    pushToken: token,
    notificationsEnabled: true,
  })
}