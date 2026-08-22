import { useSyncExternalStore } from 'react'

/**
 * Subscribes to the browser's own online state.
 *
 * <p>Module-level so the reference is stable. A function defined inside the hook
 * would be a new value on every render, and React would unsubscribe and
 * resubscribe each time.
 */
function subscribe(onStoreChange: () => void): () => void {
  window.addEventListener('online', onStoreChange)
  window.addEventListener('offline', onStoreChange)

  return () => {
    window.removeEventListener('online', onStoreChange)
    window.removeEventListener('offline', onStoreChange)
  }
}

function getSnapshot(): boolean {
  return navigator.onLine
}

/**
 * Whether the browser believes it has a network.
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