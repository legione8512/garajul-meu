/**
 * Where a native client keeps the refresh token between launches.
 *
 * <p>Section 35 deferred the plugin itself - "Capacitor secure-storage plugin,
 * selected during the mobile phase against current documentation" - and this
 * seam is what let the rest of the session model be written, reviewed and
 * tested before that choice was made. **It was made on 2026-08-31**:
 * `keystoreSecureStore.ts` names it and carries the reasoning. This interface
 * stays because it is what keeps the plugin's name in one file.
 *
 * <p>A refusing stub lived here until that day, so that a native build without
 * a plugin would fail with a sentence rather than fall back to somewhere unsafe.
 * It is gone, and what replaced it is stronger: `nativeSessionChannel` now
 * *requires* a store, so the compiler refuses what the stub could only refuse at
 * runtime.
 *
 * <p>What the implementation must be, whatever it ends up being called: backed
 * by the **Android Keystore** and the **iOS Keychain**, never by
 * SharedPreferences or UserDefaults, which are ordinary files on the device. A
 * thirty-day credential that opens every endpoint does not go in an ordinary
 * file.
 */
export interface SecureStore {
  read(): Promise<string | null>
  write(value: string): Promise<void>
  clear(): Promise<void>
}

const refusal =
  'No secure store is configured. The Capacitor secure-storage plugin is '
  + 'specification section 35\'s deferred choice and has not been made yet; '
  + 'a native build cannot hold a session until it is.'

/**
 * Refuses, loudly, and names the missing decision.
 *
 * <p>The bargain `StubOcrProvider` and `LoggingPushNotificationProvider` strike:
 * the seam exists, the thing behind it does not, and the failure is a sentence
 * rather than a silent fallback.
 *
 * <p><strong>There is deliberately no localStorage fallback.</strong> That is
 * the precise hazard this design exists to avoid, and writing one here would
 * make the web build's safety depend on nobody ever flipping a flag.
 */
export const unchosenSecureStore: SecureStore = {
  read: () => Promise.reject(new Error(refusal)),
  write: () => Promise.reject(new Error(refusal)),
  clear: () => Promise.reject(new Error(refusal)),
}