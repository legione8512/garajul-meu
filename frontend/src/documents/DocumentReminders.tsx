import { useTranslation } from 'react-i18next'

import { remindersPath, type ReminderView } from '../api/endpoints/reminders.ts'
import { useResource } from '../api/useResource.ts'
import { errorMessageKey } from '../i18n/errorKey.ts'
import { dateTimeFormatter, lineOf } from './reminderState.ts'

interface Props {
  readonly vehicleId: string
  readonly documentId: string
}

/**
 * When this document's reminders fire, and what became of the ones that have.
 *
 * <p><strong>The standing note is not a placeholder.</strong> Section 18 makes
 * push Android and iOS only, and the applications are phases 17 and 18 - so for
 * the whole of V1 web every reminder here completes having reached nothing. A
 * list that said "sent" without saying that would be telling every current user
 * something false about their own car. The same section names the honest
 * alternative, which the product already does: the dashboard always shows
 * current expiry state.
 *
 * <p>Its own request rather than a field on the document. The document endpoint
 * answers one record and this answers a schedule; joining them would make every
 * screen that reads a document pay for reminders it does not show.
 *
 * <p>The key is the instant and the offset together. Neither is unique on its
 * own - a thirty-day reminder already sent and a thirty-day reminder scheduled
 * by a later correction can both be in this list - and the pair is, because two
 * live reminders cannot be for the same offset at the same instant.
 */
export function DocumentReminders({ vehicleId, documentId }: Props) {
  const { t, i18n } = useTranslation()

  const { data, error, loading } = useResource<readonly ReminderView[]>(
    remindersPath(vehicleId, documentId),
  )

  const formatDateTime = dateTimeFormatter(i18n.language)

  return (
    <section data-card>
      <h2>{t('reminders.title')}</h2>

      <p>{t('reminders.nativeOnly')}</p>

      {loading && <p role="status">{t('common.loading')}</p>}

      {error !== null && <p role="alert">{t(errorMessageKey(error.code))}</p>}

      {data !== null && data.length === 0 && <p>{t('reminders.none')}</p>}

      {data !== null && data.length > 0 && (
        <ul>
          {data.map((reminder) => {
            const line = lineOf(reminder, formatDateTime)

            return (
              <li
                key={`${reminder.scheduledAt}-${String(reminder.offsetDays)}`}
                data-tone={line.tone}
              >
                {t(line.leadKey, line.leadValues)}
                {' '}
                {t(line.outcomeKey, line.outcomeValues)}
              </li>
            )
          })}
        </ul>
      )}
    </section>
  )
}