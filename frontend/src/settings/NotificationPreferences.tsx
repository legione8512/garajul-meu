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

/** Everything but the switch: the part the form edits and saves with its button. */
type Schedule = Omit<Preferences, 'notificationsEnabled'>

function scheduleOf(preferences: Preferences): Schedule {
  return {
    remind30Days: preferences.remind30Days,
    remind14Days: preferences.remind14Days,
    remind7Days: preferences.remind7Days,
    remind3Days: preferences.remind3Days,
    remind1Day: preferences.remind1Day,
    remindOnExpiry: preferences.remindOnExpiry,
    notificationLocalTime: preferences.notificationLocalTime,
  }
}

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

interface NotificationPreferencesProps {
  /**
   * True on a phone the server holds as able, where the section can say that
   * notifications are on *on this phone*. False on the web, where there is no
   * "this phone" to speak about.
   */
  onThisPhone: boolean
}

/**
 * What an account is told about, and when. Specification sections 12 and 16.
 *
 * <p><strong>On screen 18 since 2026-09-11, not on screen 15</strong> - moved by
 * the developer's decision, and against the specification's screen map, which
 * puts these preferences on the profile. The reason was a contradiction seen on
 * a real iPhone: notifications refused in the phone's settings, and screen 15
 * still showing "Trimite-mi notificări" ticked under a sentence saying reminders
 * arrive on this phone. One screen now says what the phone allows and, below it,
 * what the account asks for - and on a phone that refuses, only the first.
 *
 * <p><strong>The switch saves itself; the schedule saves with its button.</strong>
 * A switch that needed a second button to take effect would look off while
 * reminders were still being sent, whereas six offsets and a time are edited
 * together and sent together. With the switch off the schedule is not shown at
 * all: nothing would be sent on it.
 *
 * <p><strong>All eight fields go on every save</strong>, because the endpoint is a
 * replace and the backend refuses a body that omits a switch rather than reading
 * the gap as "off". Both saves start from what the server last said: the switch
 * sends the server's schedule with its own value changed, so an edit to the
 * schedule that has not been saved is neither sent by the switch nor lost by it;
 * the button sends the edited schedule with the server's switch. What is shown
 * after either is the server's answer.
 *
 * <p><strong>A refused switch puts itself back.</strong> Showing "off" for
 * reminders the server is still sending is the kind of lie screen 18 was rebuilt
 * on 2026-09-11 to stop telling.
 */
export function NotificationPreferences({ onThisPhone }: NotificationPreferencesProps) {
  const { t } = useTranslation()
  const { data, error, loading } = useResource<Preferences>(notificationPreferencesPath)

  const switching = useSubmission()
  const saving = useSubmission()
  const [answered, setAnswered] = useState<Preferences | null>(null)
  const [edited, setEdited] = useState<Schedule | null>(null)
  const [saved, setSaved] = useState(false)

  // Null means "what the server said when the screen opened", the arrangement
  // the vehicle nickname uses - no effect copying server state into state.
  const server = answered ?? data
  const shown = server === null ? null : { ...server, ...edited }

  async function toggle(on: boolean) {
    if (server === null || switching.pending) {
      return
    }

    const before = server
    setAnswered({ ...before, notificationsEnabled: on })

    const failure = await switching.submit(async () => {
      setAnswered(await saveNotificationPreferences({ ...before, notificationsEnabled: on }))
    })

    if (failure !== null) {
      setAnswered(before)
    }
  }

  function change(patch: Partial<Schedule>) {
    if (shown !== null) {
      setEdited({ ...scheduleOf(shown), ...patch })
      setSaved(false)
    }
  }

  async function handleSave(event: FormEvent) {
    event.preventDefault()

    if (shown === null) {
      return
    }

    const failure = await saving.submit(async () => {
      setAnswered(await saveNotificationPreferences(shown))
      setEdited(null)
    })

    if (failure === null) {
      setSaved(true)
    }
  }

  return (
    <section data-card>
      <h2>{t('notificationPreferences.title')}</h2>

      {loading && <p role="status">{t('common.loading')}</p>}

      {error !== null && <p role="alert">{t(errorMessageKey(error.code))}</p>}

      {shown !== null && (
        <>
          {!shown.notificationsEnabled && <p role="status">{t('notifications.paused')}</p>}

          {shown.notificationsEnabled && onThisPhone && (
            <p role="status">{t('notifications.granted')}</p>
          )}

          <CheckboxField
            asSwitch
            label={t('notificationPreferences.enabled')}
            checked={shown.notificationsEnabled}
            onChange={(on) => { void toggle(on) }}
          />

          <FormError error={switching.error} />

          {shown.notificationsEnabled && (
            <form onSubmit={(event) => { void handleSave(event) }} noValidate>
              <FormError error={saving.error} />

              <fieldset>
                <legend>{t('notificationPreferences.leads')}</legend>

                {OFFSETS.map((offset: Offset) => (
                  <CheckboxField
                    key={offset}
                    label={t(`notificationPreferences.${offset}`)}
                    checked={shown[offset]}
                    onChange={(checked) => { change({ [offset]: checked } as Partial<Schedule>) }}
                  />
                ))}
              </fieldset>

              <TextField
                label={t('notificationPreferences.time')}
                type="time"
                value={forInput(shown.notificationLocalTime)}
                onChange={(value) => { change({ notificationLocalTime: forWire(value) }) }}
                message={undefined}
              />

              <button type="submit" disabled={saving.pending}>
                {t('notificationPreferences.save')}
              </button>

              {saved && <p role="status">{t('notificationPreferences.saved')}</p>}
            </form>
          )}
        </>
      )}
    </section>
  )
}
