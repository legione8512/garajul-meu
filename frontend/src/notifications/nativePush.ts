import type { PluginListenerHandle } from '@capacitor/core'

import type { Push, PushPermission } from './push.ts'

/**
 * Long enough that a slow network is not mistaken for a failure, short enough
 * that screen 18 cannot sit on a spinner for ever. Registration is a round trip
 * to Google, so it is not instant even when everything works.
 */
const TOKEN_TIMEOUT_MS = 15_000

/**
 * Loaded inside the functions, for the reason measured on 2026-08-31: a static
 * import of a package that declares no `sideEffects` survives its importer being
 * tree-shaken, because `registerPlugin()` runs at module scope. The web build
 * dropped every line of the secure-storage adapter and shipped the plugin
 * anyway. Inside a function there is no top-level import to survive.
 */
/**
 * Returns the *result* of a call, never the plugin. Handing a Capacitor Proxy
 * back from an `async` function makes the runtime ask it for `.then`, which it
 * forwards to the platform as a native method, and the promise never settles.
 * See `keystoreSecureStore.ts` for what that cost.
 */
async function permissionState(ask: 'check' | 'request'): Promise<string> {
  const { PushNotifications } = await import('@capacitor/push-notifications')
  const status = ask === 'check'
    ? await PushNotifications.checkPermissions()
    : await PushNotifications.requestPermissions()

  return status.receive
}

/** See `PushPermission`: two of Capacitor's four states mean the same thing here. */
function simplified(receive: string): PushPermission {
  if (receive === 'granted') {
    return 'granted'
  }
  return receive === 'denied' ? 'denied' : 'prompt'
}

/**
 * The token arrives on an event, not as a return value, so this is the one place
 * in the application that turns a listener into a promise.
 *
 * <p>Three things have to be got right and each of them is a way to leak.
 * **Every path removes both listeners**, including the timeout and the failure
 * to register at all - otherwise a second attempt after a refusal stacks a
 * second pair and resolves the wrong promise. **Only the first outcome counts**,
 * because `registration` and `registrationError` can both arrive. And **there is
 * a timeout**, because a promise that never settles would leave screen 18
 * waiting on a spinner with nothing to say.
 *
 * <p>The registration error's text is carried into the message, and that is
 * safe: a registration that failed produced no token, so there is none to leak.
 */
async function token(): Promise<string> {
    const { PushNotifications: notifications } = await import('@capacitor/push-notifications')

  return new Promise<string>((resolve, reject) => {
    const handles: PluginListenerHandle[] = []
    let settled = false

    function finish(outcome: () => void) {
      if (settled) {
        return
      }
      settled = true
      clearTimeout(timer)
      void Promise.all(handles.map(handle => handle.remove()))
      outcome()
    }

    // Declared after `finish` reads it, which is legal and deliberate: a
    // function declaration is hoisted, and it cannot run before the line below
    // has assigned. Written as `let` first, and lint was right to refuse it.
    const timer = setTimeout(
      () => { finish(() => { reject(new Error('The push registration did not answer in time.')) }) },
      TOKEN_TIMEOUT_MS,
    )

    void (async () => {
      handles.push(await notifications.addListener('registration', (received) => {
        finish(() => { resolve(received.value) })
      }))

      handles.push(await notifications.addListener('registrationError', (failure) => {
        finish(() => { reject(new Error(`The platform refused to register: ${failure.error}`)) })
      }))

      await notifications.register()
    })().catch((failure: unknown) => {
      finish(() => { reject(failure instanceof Error ? failure : new Error(String(failure))) })
    })
  })
}

export const nativePush: Push = {
  permission: async () => simplified(await permissionState('check')),
  request: async () => simplified(await permissionState('request')),
  token,

  /**
   * `@capacitor/core` is imported here rather than at the top for the same
   * reason as the plugin above, and it matters more: `core` is what every
   * Capacitor package registers itself through, so a static import of it is the
   * one that would drag the whole runtime into the browser bundle.
   */
  async platform() {
    const { Capacitor } = await import('@capacitor/core')
    return Capacitor.getPlatform() === 'ios' ? 'IOS' : 'ANDROID'
  },
}