import type { SessionChannel } from './SessionChannel.ts'

/**
 * The browser, which owns nothing because the browser owns everything.
 *
 * <p>Every member is a no-op on purpose, and that is what keeps this change
 * invisible on the web: `present` answers null so the request body stays `{}`,
 * `remember` is handed the null the server sends a cookie client, and `forget`
 * has nothing to forget because the cookie is cleared by `Set-Cookie` from the
 * server, which is the only thing that can reach it.
 */
export const cookieSessionChannel: SessionChannel = {
  carriesTokenItself: false,
  present: () => Promise.resolve(null),
  remember: () => Promise.resolve(),
  forget: () => Promise.resolve(),
}