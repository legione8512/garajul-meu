/**
 * Where "is there a network?" is answered, and on Android that is Android.
 *
 * <p><strong>Found on 2026-09-19, on a Huawei MediaPad T3 running Android 7.0.</strong>
 * The banner saying there was no internet sat at the top of every screen while
 * signing in, loading and saving all worked. The Wi-Fi icon had no exclamation
 * mark, so Android had validated the network; the WebView answered
 * `navigator.onLine = false` all the same, for as long as the application ran.
 * `useOnline.ts` already recorded that `navigator.onLine` can be wrong the other
 * way - true on a Wi-Fi with no internet behind it. On this WebView it was wrong
 * this way, which is worse: a warning that is always on and always false
 * teaches a person to ignore every warning.
 *
 * <p>So on Android the answer comes from `@capacitor/network`, whose source was
 * read before choosing it (8.0.1). It counts a network as connected when the
 * active network has both `NET_CAPABILITY_VALIDATED` and `NET_CAPABILITY_INTERNET`
 * - Android's own internet check, the one the exclamation mark reports - so it
 * also closes the captive-portal gap `useOnline.ts` admits. Its calls exist from
 * API 23 and 24; the minimum is 24. Its manifest asks only for
 * `ACCESS_NETWORK_STATE`, which the application already carries.
 *
 * <p><strong>Three sources, and a way back.</strong>
 * - `browser` - `navigator.onLine`, as before. The web build, and iOS, where
 *   the defect was never seen and no build has the plugin yet.
 * - `pending` - between launch and Android's first answer: **online**. The
 *   tablet's WebView says offline from the first frame, so reading it here
 *   would flash the banner at every launch before Android corrected it.
 * - `android` - the plugin's latest word.
 * - and back to `browser` if the plugin fails, so a failure here can leave
 *   things no worse than they were before it existed.
 *
 * <p>Changes after launch arrive as `networkStatusChange`. The plugin stops
 * watching while the application is in the background and, on returning,
 * reports a network lost in the meantime itself; one that came back is reported
 * because re-registering with Android delivers the current network at once.
 *
 * <p>Plugins are imported inside the function and never returned - see
 * `backButton.ts`.
 */

type Source = 'browser' | 'pending' | 'android'

let source: Source = 'browser'
let androidConnected = true
const listeners = new Set<() => void>()

function settle(next: Source, connected: boolean = androidConnected): void {
  source = next
  androidConnected = connected

  for (const listener of listeners) {
    listener()
  }
}

/**
 * For `useSyncExternalStore`. The browser's own events are listened to in every
 * source, which costs nothing and keeps `browser` exactly what it was.
 */
export function subscribe(onStoreChange: () => void): () => void {
  listeners.add(onStoreChange)
  window.addEventListener('online', onStoreChange)
  window.addEventListener('offline', onStoreChange)

  return () => {
    listeners.delete(onStoreChange)
    window.removeEventListener('online', onStoreChange)
    window.removeEventListener('offline', onStoreChange)
  }
}

export function getSnapshot(): boolean {
  if (source === 'android') {
    return androidConnected
  }

  return source === 'pending' || navigator.onLine
}

/**
 * Started once, from `main.tsx`, in the native build only. Everything before
 * its first `await` runs before React renders, which is what makes the first
 * frame `pending` rather than `browser`.
 */
export async function watchConnectivity(): Promise<void> {
  settle('pending')

  try {
    const { Capacitor } = await import('@capacitor/core')

    if (Capacitor.getPlatform() !== 'android') {
      settle('browser')
      return
    }

    const { Network } = await import('@capacitor/network')

    // Listening before asking, so a change cannot fall between the two - and a
    // change heard first is newer than the answer to a question asked before it,
    // so that answer must not overwrite it.
    let heard = false

    await Network.addListener('networkStatusChange', ({ connected }) => {
      heard = true
      settle('android', connected)
    })

    const { connected } = await Network.getStatus()

    if (!heard) {
      settle('android', connected)
    }
  } catch {
    settle('browser')
  }
}
