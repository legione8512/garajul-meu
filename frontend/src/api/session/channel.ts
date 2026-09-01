import { cookieSessionChannel } from './cookieChannel.ts'
import { keystoreSecureStore } from './keystoreSecureStore.ts'
import { nativeSessionChannel } from './nativeChannel.ts'
import type { SessionChannel } from './SessionChannel.ts'

/**
 * Chosen at build time, and the deciding argument is the plugin rather than the
 * style.
 *
 * <p>Runtime detection - `Capacitor.isNativePlatform()` - is one line and would
 * work. What it also does is carry the native path, and the secure-storage
 * plugin's **web shim**, into the bundle served to browsers. Those shims
 * commonly fall back to `localStorage`, which is the one place this application
 * has decided a credential may never sit: `tokenStore.ts` keeps the access token
 * in a module variable for exactly that reason, and a thirty-day refresh token
 * is the more valuable of the two.
 *
 * <p>Choosing here means the branch not taken is dead code. Vite replaces
 * `import.meta.env.VITE_CLIENT` with a literal before the bundler runs, so the
 * unused implementation is dropped entirely rather than merely unreachable.
 *
 * <p><strong>That claim is about the artifact, so check the artifact.</strong>
 * After `npm run build`, the refusal message from `secureStore.ts` must not
 * appear anywhere in `dist/`. Same class of check as listing the jar, and this
 * project has already paid once for trusting the source over the output.
 *
 * <p>`VITE_CLIENT` is set in exactly one place: `.env.native`, which Vite loads
 * only for `vite build --mode native`. Every other build - the dev server,
 * Cloudflare Pages, the Playwright journey - leaves it unset and takes the
 * cookie channel, exactly as before. `committedEnv.test.ts` asserts the flag is
 * there, because its absence is silent: the native build would take the cookie
 * channel, the cookie would be cross-site to a WebView on `https://localhost`,
 * and the application would install, launch and refuse to sign anybody in.
 */

export const sessionChannel: SessionChannel =
  import.meta.env.VITE_CLIENT === 'native'
    ? nativeSessionChannel(keystoreSecureStore)
    : cookieSessionChannel