import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'

import {
  notificationPreferencesPath, saveNotificationPreferences,
  type NotificationPreferences as Preferences,
} from '../api/endpoints/notifications.ts'
import { useResource } from '../api/useResource.ts'
import { CheckboxField } from '../components/CheckboxField.tsx'
import { FormError } from '../components/FormError.tsx'
import { TextField } from '../components/TextField.tsx'
import { useSubmission } from '../forms/useSubmission.ts'
import { errorMessageKey } from '../i18n/errorKey.ts'

/** The six offsets, in the order they fire. */
const OFFSETS = [
  'remind30Days', 'remind14Days', 'remind7Days', 'remind3Days', 'remind1Day', 'remindOnExpiry',
] as const

type Offset = (typeof OFFSETS)[number]

/**
 * `HH:mm:ss` on the wire, `HH:mm` in the input.
 *
 * <p>Jackson writes a LocalTime with its seconds and `<input type="time">` shows
 * none, so a value round-tripped without this would either put "09:00:00" into a
 * control that refuses it or send back a time the backend reads as a different
 * one. Both directions are handled here rather than at the two call sites.
 */
function forInput(wireTime: string): string {
  return wireTime.slice(0, 5)
}

function forWire(inputTime: string): string {
  return inputTime.length === 5 ? `${inputTime}:00` : inputTime
}

/**
 * What an account is told about, on screen 15. Specification sections 12 and 16.
 *
 * <p><strong>All eight fields are sent on every save.</strong> The endpoint is a
 * replace, and the backend refuses a body that omits a switch rather than
 * reading the gap as "off" - so the form holds a whole preferences object and
 * edits it, instead of collecting only what was touched.
 *
 * <p>The standing note about push being native-only is shown here too, and it is
 * the same sentence the reminder list uses. Somebody turning these switches on
 * in a browser today is configuring something that will reach them when the
 * phone application exists and not before; a preferences screen that does not
 * say so is making a promise the system cannot keep.
 */
export function NotificationPreferences() {
  const { t } = useTranslation()
  const { data, error, loading } = useResource<Preferences>(notificationPreferencesPath)

  const save = useSubmission()
  const [draft, setDraft] = useState<Preferences | null>(null)
  const [saved, setSaved] = useState(false)

  // Null means "showing what the server last said", the same arrangement the
  // vehicle nickname uses - no effect copying server state into state.
  const preferences = draft ?? data

  function change(patch: Partial<Preferences>) {
    if (preferences !== null) {
      setDraft({ ...preferences, ...patch })
      setSaved(false)
    }
  }

  async function handleSave(event: FormEvent) {
    event.preventDefault()

    if (preferences === null) {
      return
    }

    const failure = await save.submit(async () => {
      await saveNotificationPreferences(preferences)
    })

    if (failure === null) {
      setSaved(true)
    }
  }

  return (
    <section>
      <h2>{t('notificationPreferences.title')}</h2>

      <p>{t('reminders.nativeOnly')}</p>

      {loading && <p role="status">{t('common.loading')}</p>}

      {error !== null && <p role="alert">{t(errorMessageKey(error.code))}</p>}

      {preferences !== null && (
        <form onSubmit={(event) => { void handleSave(event) }} noValidate>
          <FormError error={save.error} />

          <CheckboxField
            label={t('notificationPreferences.enabled')}
            checked={preferences.notificationsEnabled}
            onChange={(checked) => { change({ notificationsEnabled: checked }) }}
          />

          <fieldset>
            <legend>{t('notificationPreferences.leads')}</legend>

            {OFFSETS.map((offset: Offset) => (
              <CheckboxField
                key={offset}
                label={t(`notificationPreferences.${offset}`)}
                checked={preferences[offset]}
                onChange={(checked) => { change({ [offset]: checked } as Partial<Preferences>) }}
              />
            ))}
          </fieldset>

          <TextField
            label={t('notificationPreferences.time')}
            type="time"
            value={forInput(preferences.notificationLocalTime)}
            onChange={(value) => { change({ notificationLocalTime: forWire(value) }) }}
            message={undefined}
          />

          <button type="submit" disabled={save.pending}>
            {t('notificationPreferences.save')}
          </button>

          {saved && <p role="status">{t('notificationPreferences.saved')}</p>}
        </form>
      )}
    </section>
  )
}