import { useTranslation } from 'react-i18next'

import { useAuth } from '../auth/useAuth.ts'
import { isSupportedLanguage, languageNames } from '../i18n/language.ts'

/**
 * Screen 15 in specification section 5, minimal. Notification preferences,
 * changing the password or the address, and deleting the account are Phase 13 -
 * every one of them already has a backend endpoint waiting.
 *
 * <p>Signing out navigates nowhere on purpose. Ending the session makes the
 * status anonymous, RequireAuth sees that on a protected route, and the
 * departure happens by itself. Doing it by hand as well races the gate: whoever
 * moves first decides where the person lands.
 */
export function ProfilePage() {
  const { t } = useTranslation()
  const { profile, signOut } = useAuth()

  return (
    <>
      <h1>{t('screens.profile')}</h1>

      {profile === null ? null : (
        <dl>
          <dt>{t('fields.fullName')}</dt>
          <dd>{profile.fullName}</dd>

          <dt>{t('fields.email')}</dt>
          <dd>{profile.email}</dd>

          <dt>{t('language.label')}</dt>
          <dd>
            {isSupportedLanguage(profile.preferredLanguage)
              ? languageNames[profile.preferredLanguage]
              : profile.preferredLanguage}
          </dd>
        </dl>
      )}

      <button type="button" onClick={() => { void signOut() }}>{t('profile.signOut')}</button>
    </>
  )
}