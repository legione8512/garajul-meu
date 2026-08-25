import type { SecureStore } from './secureStore.ts'
import { unchosenSecureStore } from './secureStore.ts'
import type { SessionChannel } from './SessionChannel.ts'

/**
 * The Capacitor client, which has to hold the token itself.
 *
 * <p>A factory rather than a constant so a test can hand it a store, and so the
 * real plugin can be passed in from one place once section 35's choice is made,
 * without this file learning the plugin's name.
 */
export function nativeSessionChannel(store: SecureStore = unchosenSecureStore): SessionChannel {
  return {
    carriesTokenItself: true,

    present: () => store.read(),

    async remember(refreshToken: string | null): Promise<void> {
      if (refreshToken === null) {
        // This channel always asks for the token in the body, and the backend
        // always answers with it when asked. Null here means the two halves of
        // section 14's contract have drifted apart - and carrying on would leave
        // the application holding a session it can never renew, which looks
        // exactly like a successful sign-in until the access token expires ten
        // minutes later and cannot be replaced.
        throw new Error(
          'The server returned no refresh token to a client that asked for one in the body.',
        )
      }

      await store.write(refreshToken)
    },

    forget: () => store.clear(),
  }
}