import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'

import { login, logout } from '../api/endpoints/auth.ts'
import { getProfile, updateProfile, type UserProfile } from '../api/endpoints/users.ts'
import { refreshSession } from '../api/refresh.ts'
import { subscribeToAccessToken } from '../api/tokenStore.ts'
import { isSupportedLanguage, type SupportedLanguage } from '../i18n/language.ts'
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

  /**
   * Section 6: the account's preference outranks whatever this device
   * remembered. That is what makes the language follow a person from their
   * laptop to their phone instead of being a per-browser setting.
   *
   * <p>Extracted when screen 15 arrived, because a profile saved there has to
   * obey the same rule as one loaded at sign-in. Two copies of this would be one
   * place that forgets.
   */
  const applyLanguageOf = useCallback(async (loaded: UserProfile) => {
    if (isSupportedLanguage(loaded.preferredLanguage)
      && i18n.resolvedLanguage !== loaded.preferredLanguage) {
      await i18n.changeLanguage(loaded.preferredLanguage)
    }
  }, [i18n])

  const adoptSession = useCallback(async () => {
    const loaded = await getProfile()

    setProfile(loaded)
    setStatus('authenticated')

    await applyLanguageOf(loaded)
  }, [applyLanguageOf])

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
  const profileChanged = useCallback((saved: UserProfile) => {
    setProfile(saved)
    void applyLanguageOf(saved)
  }, [applyLanguageOf])

  /**
   * Choosing a language, and making the choice outlast the page.
   *
   * <p><strong>The interface changes first and the account is told after.</strong>
   * The other order would hold the whole application on a network round trip to
   * redraw one word, so somebody who has just clicked their own language would
   * watch nothing happen for as long as the request took.
   *
   * <p><strong>Signed in, telling the account is not optional.</strong>
   * `applyLanguageOf` runs on every load and applies `preferred_language` over
   * whatever this device remembered - that is section 6, and it is what carries a
   * language from a laptop to a phone. A switcher that stopped at i18next was
   * therefore offering a choice the next reload would quietly reverse, storage
   * included. Writing through is what makes the header control mean the same
   * thing as screen 15.
   *
   * <p><strong>A refused write is swallowed on purpose.</strong> What is lost is
   * durability, not the choice: the application stays in the chosen language for
   * as long as this page lives, which is precisely the behaviour that existed
   * before. The header has no error region, and inventing one for this would put
   * a failure notice above every screen over a preference.
   */
  const chooseLanguage = useCallback(async (language: SupportedLanguage) => {
    await i18n.changeLanguage(language)

    // 'unknown' lands here too, and sending nothing is right for it: the session
    // has not answered yet, so there is no account to tell.
    if (status !== 'authenticated') {
      return
    }

    try {
      profileChanged(await updateProfile({ preferredLanguage: language }))
    } catch {
      // Deliberately silent - see above.
    }
  }, [i18n, status, profileChanged])

  const value = useMemo<AuthValue>(
    () => ({ status, profile, signIn, signOut, profileChanged, chooseLanguage }),
    [status, profile, signIn, signOut, profileChanged, chooseLanguage],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}