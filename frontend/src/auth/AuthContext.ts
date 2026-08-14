import { createContext } from 'react'

import type { UserProfile } from '../api/endpoints/users.ts'

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
}

/** Null outside a provider, which is what lets useAuth fail loudly. */
export const AuthContext = createContext<AuthValue | null>(null)