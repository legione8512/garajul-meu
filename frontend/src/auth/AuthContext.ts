import { createContext } from 'react'

import type { UserProfile } from '../api/endpoints/users.ts'
import type { SupportedLanguage } from '../i18n/language.ts'

/**
 * Three states, not two.
 *
 * <p>`unknown` covers the moment between the page loading and the silent
 * refresh answering. Collapsing it into `anonymous` would render the signed-out
 * view first and correct itself a moment later, so somebody who is perfectly
 * signed in watches the application tell them they are not.
 */
export type AuthStatus = 'unknown' | 'authenticated' | 'anonymous'

export interface AuthValue {
  status: AuthStatus
  profile: UserProfile | null
  signIn: (email: string, password: string) => Promise<void>
  signOut: () => Promise<void>
  /**
   * What a screen calls after the backend has confirmed a change to the account.
   *
   * <p>Exists so the profile stays in one place. A screen keeping its own saved
   * copy would leave two profiles free to disagree, and the language would move
   * in the context while the interface went on rendering the old one - the rule
   * that the account's preference outranks the device is applied here and
   * nowhere else.
   */
  profileChanged: (profile: UserProfile) => void
    /**
   * Choosing a language, from wherever the choice is offered.
   *
   * <p>Lives here rather than in the switcher because the rule it has to obey
   * lives here. Section 6 makes the account's `preferred_language` outrank
   * whatever this device remembered, so a control that only told i18next was
   * offering a choice the next page load would silently undo - which is exactly
   * what the header switcher did until 2026-08-31.
   */
  chooseLanguage: (language: SupportedLanguage) => Promise<void>
}

/** Null outside a provider, which is what lets useAuth fail loudly. */
export const AuthContext = createContext<AuthValue | null>(null)