import { useSyncExternalStore } from 'react'

import { getSnapshot, subscribe } from './connectivity.ts'

/**
 * Whether there is a network - asked of Android on Android, and of the browser
 * everywhere else.
 *
 * <p><strong>The answer lives in `connectivity.ts` since 2026-09-19</strong>, when
 * an Android 7.0 WebView reported itself offline for as long as the application
 * ran, on a network Android had validated. That file says why, and why Android
 * only. What follows is about the browser's answer, which the web build and iOS
 * still use. Both functions there are module-level so their references are
 * stable: defined inside this hook they would be new on every render, and React
 * would unsubscribe and resubscribe each time.
 *
 * <p>Specification section 25: V1 is online-required, and the UI "must detect
 * network loss and show clear retry/offline states instead of silently
 * failing". This is the detection half; the retry half is `useResource.reload`,
 * which every read screen already offers.
 *
 * <p><strong>useSyncExternalStore rather than useState with an effect.</strong>
 * The browser's connectivity is an external store, which is precisely what this
 * hook is for. The hand-written version had a real gap - the connection can drop
 * between the first render and the effect that attaches the listeners, and no
 * event is heard for a change that happened before anyone was listening - and
 * closing it meant calling setState inside the effect, which
 * react-hooks/set-state-in-effect refuses for good reason. React closes the same
 * gap itself here by re-reading the snapshot after subscribing, so the fix is to
 * use the right tool rather than to compensate for the wrong one.
 *
 * <p>No `getServerSnapshot`: this application renders only in a browser. If
 * server rendering is ever added, its absence becomes an error at that moment
 * rather than a wrong answer, which is the failure mode worth having.
 *
 * <p><strong>`navigator.onLine` is honest about less than it sounds.</strong> It
 * reports whether the device has a network interface up, not whether anything is
 * reachable - so it stays true on a wifi with no internet behind it, and behind
 * a captive portal. It catches the obvious cases, which are also the common
 * ones: aeroplane mode, a cable pulled, a phone leaving coverage. The gap is
 * deliberate rather than unnoticed: closing it would mean polling the backend on
 * a timer, real traffic on every client forever, to detect a condition the
 * banner cannot fix anyway. A request that fails while the browser thinks it is
 * online still surfaces as an error on the screen that made it.
 */
export function useOnline(): boolean {
  return useSyncExternalStore(subscribe, getSnapshot)
}