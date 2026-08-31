import { useMemo, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'

import { updateProfile, type UserProfile } from '../api/endpoints/users.ts'
import { ComboboxField } from '../components/ComboboxField.tsx'
import { FormError } from '../components/FormError.tsx'
import { SelectField, type SelectOption } from '../components/SelectField.tsx'
import { TextField } from '../components/TextField.tsx'
import { useAuth } from '../auth/useAuth.ts'
import { GENERIC_MESSAGE, maxLength, required, type ValidationMessage } from '../forms/rules.ts'
import { useSubmission } from '../forms/useSubmission.ts'
import { validate } from '../forms/validate.ts'
import { languageNames, supportedLanguages } from '../i18n/language.ts'
import { paths } from '../routes/paths.ts'
import { NotificationPreferences } from '../settings/NotificationPreferences.tsx'

const nameRules = { fullName: [required, maxLength(150)] }

const languageOptions: readonly SelectOption[] = supportedLanguages.map(language => ({
  value: language,
  label: languageNames[language],
}))

/**
 * Every time zone the runtime knows, which is exactly what the backend validates
 * against - it checks membership of `ZoneId.getAvailableZoneIds()`.
 *
 * <p>A hand-written list of "the ones Romanians use" would be a second source of
 * truth, wrong for anybody abroad and stale the first time the tz database
 * moves. Where `supportedValuesOf` is missing the account's own value stands
 * alone, which is honest: the control then changes nothing rather than offering
 * a choice built out of guesses.
 */
function timezoneOptions(current: string): readonly SelectOption[] {
  const available = typeof Intl.supportedValuesOf === 'function'
    ? Intl.supportedValuesOf('timeZone')
    : [current]

  const zones = available.includes(current) ? available : [current, ...available]

  return zones.map(zone => ({ value: zone, label: zone }))
}

/**
 * Screen 15 in specification section 5.
 *
 * <p>The address is shown and not edited here. Changing it is screen 17, because
 * it is not a field but a flow - a code goes to the old inbox and the new
 * address arrives unverified - and putting it in this form would suggest it
 * saves like a name. The password and the account's own deletion are separate
 * screens for the same reason: each is a decision, not a setting.
 *
 * <p>Signing out navigates nowhere on purpose. Ending the session makes the
 * status anonymous, RequireAuth sees that on a protected route, and the
 * departure happens by itself. Doing it by hand as well races the gate: whoever
 * moves first decides where the person lands.
 */
export function ProfilePage() {
  const { t } = useTranslation()
  const { profile, signOut, profileChanged } = useAuth()

  const save = useSubmission()
  const [draft, setDraft] = useState<Partial<UserProfile> | null>(null)
  const [message, setMessage] = useState<ValidationMessage | undefined>(undefined)
  const [zoneMessage, setZoneMessage] = useState<ValidationMessage | undefined>(undefined)
  const [saved, setSaved] = useState(false)

  const fullName = draft?.fullName ?? profile?.fullName ?? ''
  const language = draft?.preferredLanguage ?? profile?.preferredLanguage ?? ''
  const timezone = draft?.timezone ?? profile?.timezone ?? ''

  // Several hundred entries, and `supportedValuesOf` is not free. It used to be
  // rebuilt on every keystroke in the name field, which nothing noticed while
  // the control was a select and would be felt now that it filters as you type.
  const zones = useMemo(() => timezoneOptions(profile?.timezone ?? ''), [profile?.timezone])

  function change(patch: Partial<UserProfile>) {
    setDraft({ fullName, preferredLanguage: language, timezone, ...patch })
    setSaved(false)
  }

  async function handleSave(event: FormEvent) {
    event.preventDefault()

    const broken = validate({ fullName }, nameRules)
    setMessage(broken.fullName)

    // A datalist suggests and does not constrain, so anything can be typed here.
    // The backend refuses an unknown zone anyway; this only answers sooner.
    const unknownZone = !zones.some(zone => zone.value === timezone)
    setZoneMessage(unknownZone ? GENERIC_MESSAGE : undefined)

    if (broken.fullName !== undefined || unknownZone) {
      return
    }

    let confirmed: UserProfile | null = null

    const failure = await save.submit(async () => {
      confirmed = await updateProfile({
        fullName,
        preferredLanguage: language,
        timezone,
      })
    })

    if (failure === null && confirmed !== null) {
      // Handed to the context rather than kept here, so the language change the
      // account just asked for reaches the interface as well as the database.
      profileChanged(confirmed)
      setDraft(null)
      setSaved(true)
    }
  }

  return (
    <>
      <h1>{t('screens.profile')}</h1>

      {profile === null ? null : (
        <>
          <form data-card onSubmit={(event) => { void handleSave(event) }} noValidate>
            <h2>{t('profile.account')}</h2>

            {/*
              The address was sitting above the heading of the section it belongs
              to, which read as a stray fact about nobody in particular. It is
              shown rather than edited here because changing it needs its own
              screen and a password.
            */}
            <dl>
              <dt>{t('fields.email')}</dt>
              <dd>
                {profile.email}
                {' '}
                {profile.emailVerified
                  ? t('profile.emailVerified')
                  : t('profile.emailNotVerified')}
              </dd>
            </dl>

            <FormError error={save.error} />

            <TextField
              label={t('fields.fullName')}
              value={fullName}
              onChange={(value) => { change({ fullName: value }) }}
              message={message}
              autoComplete="name"
              maxLength={150}
            />

            <SelectField
              label={t('language.label')}
              value={language}
              options={languageOptions}
              onChange={(value) => { change({ preferredLanguage: value }) }}
            />

            <ComboboxField
              label={t('fields.timezone')}
              value={timezone}
              options={zones}
              onChange={(value) => { change({ timezone: value }) }}
              message={zoneMessage}
            />

            <button type="submit" disabled={save.pending}>{t('profile.save')}</button>

            {saved && <p role="status">{t('profile.saved')}</p>}
          </form>

          <NotificationPreferences />

          <section data-card>
            <h2>{t('profile.security')}</h2>
            <p data-actions>
              <Link data-action="secondary" to={paths.changePassword}>
                {t('profile.changePassword')}
              </Link>
              <Link data-action="secondary" to={paths.changeEmail}>
                {t('profile.changeEmail')}
              </Link>
            </p>
            {/*
              Deleting an account is not one of three equal choices. It keeps its
              own line and the quiet treatment the document list already gives a
              destructive action - reachable, never inviting.
            */}
            <p><Link data-action="quiet" to={paths.deleteAccount}>
              {t('profile.deleteAccount')}
            </Link></p>
          </section>
        </>
      )}

      {/*
        Signing out is not what somebody came to the profile to do. Quiet, like
        the other things on this page that are reachable without being invited.
      */}
      <button data-quiet type="button" onClick={() => { void signOut() }}>
        {t('profile.signOut')}
      </button>
    </>
  )
}