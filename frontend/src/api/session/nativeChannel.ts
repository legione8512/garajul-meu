import type { SecureStore } from './secureStore.ts'
import type { SessionChannel } from './SessionChannel.ts'

/**
 * The Capacitor client, which has to hold the token itself.
 *
 * <p>A factory rather than a constant so a test can hand it a store, and so the
 * real plugin is passed in from one place - `channel.ts` - without this file
 * ever learning the plugin's name.
 *
 * <p><strong>The store is required, and it used to have a refusing default.</strong>
 * That default existed while section 35's choice was open; once
 * `keystoreSecureStore` existed it became dead code that still shipped, and the
 * string "No secure store is configured" was found in the native bundle by the
 * artifact check. Requiring the parameter deletes it and moves the rule the stub
 * enforced - a native channel must have somewhere safe to put the token - from
 * runtime to compile time.
 */
export function nativeSessionChannel(store: SecureStore): SessionChannel {
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