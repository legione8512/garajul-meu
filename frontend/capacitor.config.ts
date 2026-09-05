import type { CapacitorConfig } from '@capacitor/cli'

/**
 * The native shell's configuration. Specification section 18.
 *
 * <p><strong>`appId` is `ro.in.garaj.app`, and it is permanent.</strong> Google
 * Play keys a listing on the package name for the life of the application - it
 * cannot be changed after the first upload, and a rename means a new listing
 * with no installs and no reviews. It was chosen on 2026-08-29 against
 * `in-garaj.ro`, a domain bought to back the namespace, because reverse-DNS
 * asks for a domain somebody actually controls.
 *
 * <p><strong>It was `ro.in_garaj.app` until 2026-09-05, and between them the
 * two platforms allow neither separator.</strong> Android's `applicationId`
 * takes `[a-zA-Z0-9_]` and no hyphen - which is why the underscore was chosen
 * in the first place, and the note saying so was correct as far as it went.
 * Apple's bundle identifier takes letters, digits, periods and **hyphens**, and
 * no underscore. The intersection is alphanumeric, so `in-garaj` and `in_garaj`
 * are both impossible. Firebase refused to register the Apple app and is the
 * reason this was found.
 *
 * <p>A period is the separator both platforms do accept, so the word simply
 * became two segments. Each one still starts with a letter, which Android
 * requires. **`in` is a Kotlin keyword and not a Java one**, checked before
 * choosing: this project compiles no Kotlin at all - no `.kt` file, no Kotlin
 * Gradle plugin, and the Firebase messaging plugin's Android source is Java -
 * so nothing is affected today. If Kotlin ever arrives here, an import naming
 * this package needs backticks: ``ro.`in`.garaj.app``. That is the whole cost,
 * and it was taken knowingly against a name that reads as what it is.
 *
 * <p><strong>Changed at the last moment it was free.</strong> Nothing is
 * published to either store and Android had never been run, so this cost an
 * afternoon; after a first upload it could not have been done at all. Anything
 * calling itself permanent is worth checking on both platforms *before* the
 * upload that makes it so.
 *
 * <p>The Firebase apps registered under GCP project `garajul-meu-505722` carry
 * this same identifier - the Android one was re-registered on 2026-09-05 - and
 * `google-services.json` and `GoogleService-Info.plist` are matched to it. They
 * must never drift.
 *
 * <p>`webDir` is `dist` because that is what `vite build` writes. Capacitor
 * copies from it rather than serving it, so a stale `dist` ships a stale
 * application with no warning - `npx cap sync` after every build, which is why
 * the npm scripts below pair them.
 */
const config: CapacitorConfig = {
  appId: 'ro.in.garaj.app',
  appName: 'Garajul Meu',
  webDir: 'dist',

  /*
   * The WebView's own background, which is white by default and shows in the
   * gap between the launch screen going away and the page painting its first
   * frame. The first iOS run opened on a white screen, and the launch screen was
   * the wrong suspect: `LaunchScreen.storyboard` was given this colour and the
   * white stayed, because the white was never the launch screen at all.
   *
   * The same `#151321` as the storyboard, the Android splash and `body`. Four
   * places now carry it, and the point of all four is that nobody ever sees a
   * transition between them.
   *
   * Global rather than under `ios`, because Android has the same gap and the
   * same answer.
   */
  backgroundColor: '#151321',

    /*
   * Required by @capacitor-firebase/messaging, and it is not optional: without
   * it Swift Package Manager hits a package identity collision and the iOS build
   * fails. Needs Capacitor CLI 8.4.0 or later, and we are on 8.5.1.
   */
  experimental: {
    ios: {
      spm: {
        packageOptions: {
          '@capacitor-firebase/messaging': { symlink: true },
        },
      },
    },
  },

  server: {
    /*
     * The WebView's origin, and the reason `application-prod.yml` allows
     * `https://localhost` alongside `https://app.cyber-half.com`.
     *
     * Android's default Capacitor scheme is `http://localhost`, which modern
     * WebViews treat as an insecure origin - and an insecure origin loses the
     * camera, which is the one permission this application cannot do without.
     * `https` here is not about transport (nothing leaves the device to reach
     * the WebView); it is about being a secure context.
     */
    androidScheme: 'https',

    /*
     * There is deliberately no `iosScheme` beside it, and it is not an
     * oversight. The CLI's own documentation says the iOS scheme "can't be set
     * to schemes that the WKWebView already handles, such as http or https", so
     * the origin there is `capacitor://localhost` and no configuration changes
     * it. That is why `application-prod.yml` names two WebView origins rather
     * than one: Android was given a scheme, iOS could only be accommodated.
     */
  },
}

export default config