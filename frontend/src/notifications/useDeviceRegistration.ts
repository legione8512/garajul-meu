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
 * <p><strong>A failure here is swallowed, and screen 18 is where it
 * surfaces.</strong> There is no place on every screen to report it, and
 * reminders quietly not arriving is exactly the symptom. Until 2026-09-11 screen
 * 18 reported the *permission* rather than the registration, so a granted
 * permission whose registration had failed read as healthy. It now runs the same
 * report and says notifications are on only when the server's answer does. What
 * stays silent is a failure nobody goes to look at.
 */
export function useDeviceRegistration(): void {
  useEffect(() => {
    void reportDevice().catch(() => {
      // See above: silent here, and visible on screen 18.
    })
  }, [])
}