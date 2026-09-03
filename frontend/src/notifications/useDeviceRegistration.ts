import { useEffect } from 'react'

import { reportDevice } from './reportDevice.ts'

/**
 * Tells the account what this phone can currently do, once per launch.
 *
 * <p>Called from `AppLayout` rather than from `AuthProvider`, which is about who
 * is signed in and not about phones. That frame renders only behind
 * `RequireAuth` and stays mounted while the screens inside it change, so "once
 * per launch" is what it means without anybody having to remember it.
 *
 * <p>The rules it obeys - report every launch, say nothing before the person has
 * been asked, register a refusal too - live in `reportDevice` and are tested
 * there. This hook is only the *when*.
 *
 * <p><strong>A failure here is swallowed, and that is a known gap rather than a
 * decision to be pleased with.</strong> There is no place on every screen to
 * report it, and reminders quietly not arriving is exactly the symptom. Screen
 * 18 is where somebody would look, and it currently reports the *permission*
 * rather than the registration - so a granted permission whose registration
 * failed reads as healthy. <strong>TRIGGER: the first report of reminders not
 * arriving on a phone whose notifications are switched on.</strong> The fix is
 * for screen 18 to show what the account holds rather than what the operating
 * system allows.
 */
export function useDeviceRegistration(): void {
  useEffect(() => {
    void reportDevice().catch(() => {
      // See above. Deliberately silent, and deliberately recorded as a gap.
    })
  }, [])
}