import { nativePush } from './nativePush.ts'

/**
 * Three states, where Capacitor reports four.
 *
 * <p>`prompt` and `prompt-with-rationale` are collapsed on purpose. They differ
 * only in whether the platform thinks an explanation is owed first, and this
 * application shows one either way - screen 18 exists to be that explanation.
 * Keeping them apart would offer a choice with the same answer on both sides.
 *
 * <p>`denied` is the state worth respecting. On Android the system dialog is
 * asked **once**; after a refusal the application can never raise it again, and
 * the only remaining route is the phone's own settings. A screen that keeps
 * offering a button in that state is lying about what the button does.
 */
export type PushPermission = 'prompt' | 'granted' | 'denied'

export interface Push {
  /** What the operating system currently allows, asking nobody. */
  permission(): Promise<PushPermission>

  /** Raises the system dialog if it can still be raised, and reports the answer. */
  request(): Promise<PushPermission>

  /**
   * Registers with the platform and resolves with the token to send to the
   * backend. Rejects rather than resolving empty, because a registration that
   * produced no token is a failure and treating it as "no notifications" would
   * hide it.
   */
  token(): Promise<string>

  /**
   * Which of section 10.7's two platforms this installation is.
   *
   * <p>Asked rather than assumed, even though only Android exists today. Phase
   * 18 is iOS and the column already has both values; hardcoding `ANDROID` here
   * would be a line that works until the day it silently mislabels every iPhone
   * in the table, and nothing would fail when it did.
   */
  platform(): Promise<'ANDROID' | 'IOS'>
}

/**
 * `null` on the web, and that is the honest value rather than a stub: section 18
 * makes push native-only and V1 implements no Firebase Web Push, so there is
 * nothing for a browser to stand in for. The same shape as `images/camera.ts`,
 * and chosen at build time for the same reason.
 */
export const push: Push | null =
  import.meta.env.VITE_CLIENT === 'native' ? nativePush : null