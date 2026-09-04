import type { CapacitorConfig } from '@capacitor/cli'

/**
 * The native shell's configuration. Specification section 18.
 *
 * <p><strong>`appId` is `ro.in_garaj.app`, and it is permanent.</strong> Google
 * Play keys a listing on the package name for the life of the application - it
 * cannot be changed after the first upload, and a rename means a new listing
 * with no installs and no reviews. It was chosen on 2026-08-29 against
 * `in-garaj.ro`, a domain bought to back the namespace, because reverse-DNS
 * asks for a domain somebody actually controls. The underscore is not a slip:
 * a package segment may not contain a hyphen, and `in-garaj` would not compile.
 *
 * <p>The Firebase Android app registered under GCP project
 * `garajul-meu-505722` carries this same identifier, and `google-services.json`
 * is matched to it. The two must never drift.
 *
 * <p>`webDir` is `dist` because that is what `vite build` writes. Capacitor
 * copies from it rather than serving it, so a stale `dist` ships a stale
 * application with no warning - `npx cap sync` after every build, which is why
 * the npm scripts below pair them.
 */
const config: CapacitorConfig = {
  appId: 'ro.in_garaj.app',
  appName: 'Garajul Meu',
  webDir: 'dist',

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