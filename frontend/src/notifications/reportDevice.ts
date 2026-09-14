import { registerDevice, type DeviceView } from '../api/endpoints/devices.ts'

import { push, type Push } from './push.ts'

/**
 * The token this installation last registered, so a refusal can be reported
 * without minting anything.
 *
 * <p>Not a secret - the server already has it, and it travels in every
 * registration - so ordinary storage rather than the Keychain, which is for the
 * refresh credential and should stay uncrowded.
 */
const REGISTERED_TOKEN = 'garajul-meu.push-token'

/**
 * How long the platform may take to issue a token before its silence counts as
 * a failure.
 *
 * <p><strong>`getToken()` can wait for ever on iOS.</strong> It waits for an APNs
 * registration, and when that registration fails the plugin hears nothing:
 * `63db063` found it on an iPhone 12 Pro whose build carried no push
 * entitlement. Unbounded, the launch report hangs where nobody sees it, and
 * screen 18 would say "activăm" for good - the same silence the screen exists
 * to end. Thirty seconds is a bound rather than a measurement: generous for a
 * registration that works, and short enough that somebody watching the screen
 * is still there when it gives up.
 */
const TOKEN_TIMEOUT_MS = 30_000

/**
 * The pause before asking the platform for a token again, inside the same
 * {@link TOKEN_TIMEOUT_MS}.
 *
 * <p><strong>Found on 2026-09-14, on an iPad the application had just been
 * installed on.</strong> Screen 18 asked for the permission, was given it, and
 * showed the red "try again later" at once; going back and opening the screen
 * again showed notifications on. `getToken()` had not hung - it had refused.
 * Firebase declines an FCM token until APNs has handed the application its
 * device token, answering "No APNS token specified before fetching FCM Token"
 * straight away (read in FIRMessagingTokenManager.m, Firebase iOS SDK 12.19.1),
 * and on a fresh installation APNs answers some seconds after launch. By the
 * second visit it had. The iPhone never showed this; nothing recorded says why,
 * and the likeliest reason is that APNs had already answered by the time the
 * screen was opened.
 *
 * <p>So a refusal is asked again rather than reported, and every refusal is:
 * matching on Firebase's wording would tie this file to text Firebase is free to
 * change, and a failure that is not transient costs only the wait before the
 * same error. Two seconds is a bound rather than a measurement - long enough not
 * to call the plugin in a tight loop, short enough that a registration arriving
 * a moment after the refusal is used by the very next attempt.
 */
const TOKEN_RETRY_MS = 2_000

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

/** `token`, or a rejection once `ms` have passed without one. */
function withinDeadline(token: Promise<string>, ms: number): Promise<string> {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      reject(new Error(`The platform issued no push token within ${TOKEN_TIMEOUT_MS} ms`))
    }, ms)

    token.then(
      (value) => {
        clearTimeout(timer)
        resolve(value)
      },
      (reason: unknown) => {
        clearTimeout(timer)
        reject(reason)
      },
    )
  })
}

function pause(ms: number): Promise<void> {
  return new Promise((resolve) => {
    setTimeout(resolve, ms)
  })
}

/**
 * The platform's token, asked again after each refusal until one arrives or
 * {@link TOKEN_TIMEOUT_MS} has passed since the first request.
 *
 * <p>One deadline for all the attempts rather than one per attempt: a request
 * that never settles uses up the whole of it, exactly as before retrying
 * existed, and a string of quick refusals ends when no further attempt fits. The
 * rejection is the last one the platform gave, or the timeout.
 */
async function tokenWithinDeadline(device: Push): Promise<string> {
  const deadline = Date.now() + TOKEN_TIMEOUT_MS

  for (;;) {
    try {
      return await withinDeadline(device.token(), deadline - Date.now())
    }
    catch (reason) {
      if (Date.now() + TOKEN_RETRY_MS >= deadline) {
        throw reason
      }
      await pause(TOKEN_RETRY_MS)
    }
  }
}

/**
 * The report in progress, if there is one.
 *
 * <p>Two callers can ask at the same moment: the launch report from `AppLayout`,
 * and screen 18 opened before that report has finished. They are asking the same
 * question, so the second gets the first one's answer rather than a second
 * upsert of the same token racing it to the server. Only a report in progress
 * is shared - once it has answered, the next caller asks afresh, because the
 * answer can have changed.
 */
let inFlight: Promise<DeviceView | null> | null = null

/**
 * Tells the account what this installation can currently do, on every launch,
 * and answers with what the account now holds for it.
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
 * <p><strong>The answer, added 2026-09-11, is what screen 18 believes.</strong>
 * It resolves with the server's own description of this device once the
 * registration has been accepted, and with `null` when nothing was reported - a
 * browser, a question not yet put, a refusal with nothing to correct. Screen 18
 * says notifications are on only when this answer does: the permission alone is
 * what the operating system allows, not what the account can deliver to.
 *
 * <p>TRIGGER for revisiting: a device whose notifications were switched off in
 * settings still showing as able. That would mean the remembered token was lost
 * - cleared site data, a reinstall - and the revocation had nothing to report
 * against.
 */
export function reportDevice(): Promise<DeviceView | null> {
  inFlight ??= report().finally(() => {
    inFlight = null
  })
  return inFlight
}

async function report(): Promise<DeviceView | null> {
  const device = push

  if (device === null) {
    return null
  }

  const permission = await device.permission()

  if (permission === 'prompt') {
    return null
  }

  if (permission === 'denied') {
    const previous = remembered()

    if (previous === null) {
      return null
    }

    return registerDevice({
      platform: await device.platform(),
      pushToken: previous,
      notificationsEnabled: false,
    })
  }

  const token = await tokenWithinDeadline(device)
  remember(token)

  return registerDevice({
    platform: await device.platform(),
    pushToken: token,
    notificationsEnabled: true,
  })
}
