/**
 * What the Android Back button does: go back a screen, and leave the
 * application only from the first one.
 *
 * <p><strong>Found on 2026-09-18, on a Moto G6 Plus running Android 9</strong> -
 * the first Android device this application ever ran on. From the landing page,
 * opening another screen and pressing Back closed the whole application. That is
 * an Activity's own default when nothing claims the button: the WebView keeps a
 * history of every screen, and nobody asked it. iOS has no Back button, which is
 * why the iPhone and the iPad never met this.
 *
 * <p><strong>`@capacitor/app` alone would have traded one defect for
 * another.</strong> Read in its `AppPlugin.java` (8.1.1) before choosing it: with
 * no `backButton` listener registered, the plugin goes back when the WebView can,
 * and does nothing at all when it cannot - and its callback is enabled, so it has
 * already consumed the press. On the first screen Back would have become a
 * button that does nothing, for good. Registering a listener is what hands that
 * last press back to the platform.
 *
 * <p><strong>The first screen minimises rather than exits.</strong>
 * `minimizeApp` is `moveTaskToBack`, which is what Android itself has done with
 * a root activity since version 12: the application leaves the screen and stays
 * warm, so coming back is where the person left rather than a cold start that
 * signs them in again. `exitApp` calls `finish()`, the behaviour Android moved
 * away from. On the Android 9 phone this was found on, minimising is also what
 * makes it behave like every newer one.
 *
 * <p><strong>The plugin registers on the dispatcher, not on
 * `onBackPressed()`</strong> - also read in the source. That matters because the
 * project targets SDK 36, and an application targeting Android 16 no longer has
 * `onBackPressed()` called at all. A plugin on the old API would have fixed this
 * phone and broken the newest ones.
 *
 * <p><strong>Android only.</strong> iOS has no Back button and never fires the
 * event, so registering there would load a plugin for nothing - and would fail at
 * startup on any iOS build made before `cap sync ios` has added the plugin, which
 * no build has yet: the Mac was handed back on 2026-09-17.
 *
 * <p>Going back is `history.back()` rather than a router call. React Router
 * listens for the `popstate` that follows, and the `canGoBack` the plugin sends is
 * the WebView's own, which counts every `pushState` the router made - so the two
 * agree about where "back" is without this file needing the router at all. That
 * is also why it can run before React mounts.
 *
 * <p>Both plugins are imported inside the function, never at the top and never
 * returned: see `nativeCamera.ts` for the top-level import that survives
 * tree-shaking into the web bundle, and `keystoreSecureStore.ts` for the plugin
 * Proxy that hangs any async function which returns it.
 */
export async function handleBackButton(): Promise<void> {
  const { Capacitor } = await import('@capacitor/core')

  if (Capacitor.getPlatform() !== 'android') {
    return
  }

  const { App } = await import('@capacitor/app')

  await App.addListener('backButton', ({ canGoBack }) => {
    if (canGoBack) {
      window.history.back()
      return
    }

    void App.minimizeApp()
  })
}
