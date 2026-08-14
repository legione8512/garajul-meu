import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'

import { login, logout } from '../api/endpoints/auth.ts'
import { getProfile, type UserProfile } from '../api/endpoints/users.ts'
import { refreshSession } from '../api/refresh.ts'
import { subscribeToAccessToken } from '../api/tokenStore.ts'
import { isSupportedLanguage } from '../i18n/language.ts'
import { AuthContext, type AuthStatus, type AuthValue } from './AuthContext.ts'

/**
 * Holds who is signed in, and restores that on load.
 *
 * <p>Deliberately does <strong>not</strong> withhold its children while the
 * status is unknown. The public pages are perfectly renderable during that
 * moment, and blanking them would trade a brief wrong view for a brief empty
 * one. Gating belongs to the routes that actually need protecting, which is
 * Phase 6.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const { i18n } = useTranslation()
  const [status, setStatus] = useState<AuthStatus>('unknown')
  const [profile, setProfile] = useState<UserProfile | null>(null)
  const restoreStarted = useRef(false)

  const adoptSession = useCallback(async () => {
    const loaded = await getProfile()

    setProfile(loaded)
    setStatus('authenticated')

    // Section 6: the account's preference outranks whatever this device
    // remembered. That is what makes the language follow a person from their
    // laptop to their phone instead of being a per-browser setting.
    if (isSupportedLanguage(loaded.preferredLanguage)
      && i18n.resolvedLanguage !== loaded.preferredLanguage) {
      await i18n.changeLanguage(loaded.preferredLanguage)
    }
  }, [i18n])

  useEffect(() => {
    // Runs once. StrictMode invokes effects twice in development on purpose,
    // and refreshSession would collapse the two into one request anyway - but
    // there is no reason to do the work twice to find that out.
    if (restoreStarted.current) {
      return
    }
    restoreStarted.current = true

    void (async () => {
      if (!(await refreshSession())) {
        setStatus('anonymous')
        return
      }

      try {
        await adoptSession()
      } catch {
        // The cookie was good enough to refresh but the profile would not load.
        // Treating that as signed in would leave the application in a state it
        // has no data for.
        setStatus('anonymous')
      }
    })()
  }, [adoptSession])

  useEffect(() => subscribeToAccessToken((token) => {
    // Catches a session ending behind some unrelated request - a refresh that
    // failed while the user was reading a page - rather than waiting for them
    // to click something and be surprised.
    if (token === null) {
      setStatus((current) => (current === 'authenticated' ? 'anonymous' : current))
      setProfile(null)
    }
  }), [])

  const signIn = useCallback(async (email: string, password: string) => {
    await login(email, password)
    await adoptSession()
  }, [adoptSession])

  const signOut = useCallback(async () => {
    await logout()
    setProfile(null)
    setStatus('anonymous')
  }, [])

  const value = useMemo<AuthValue>(
    () => ({ status, profile, signIn, signOut }),
    [status, profile, signIn, signOut],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}