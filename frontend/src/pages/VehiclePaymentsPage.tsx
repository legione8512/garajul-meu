import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router'

import {
  addPayment, correctPayment, deletePayment, paymentsPath, type PaymentDetails,
} from '../api/endpoints/payments.ts'
import { useResource } from '../api/useResource.ts'
import { FormError } from '../components/FormError.tsx'
import { dateFormatter } from '../documents/status.ts'
import { useSubmission } from '../forms/useSubmission.ts'
import { countForm } from '../i18n/countForm.ts'
import { errorMessageKey } from '../i18n/errorKey.ts'
import { EMPTY_PAYMENT, leadKey, valuesOf } from '../payments/fields.ts'
import { PaymentForm } from '../payments/PaymentForm.tsx'
import { dueStateOf } from '../payments/paymentState.ts'
import { paths } from '../routes/paths.ts'

/**
 * A vehicle's recurring payments, 1.1 (docs/DESIGN_1_1_INSTALMENTS.md): the
 * list, a correction in place, and a form to add one.
 *
 * <p>A screen of its own reached from the vehicle, as the documents are,
 * rather than a section of the vehicle's screen - which holds the
 * certificate's details, the photograph and two forms already.
 *
 * <p>Deletion confirms in place, as everywhere else here: window.confirm is
 * written in the browser's language, not the application's.
 */
export function VehiclePaymentsPage() {
  const { t, i18n } = useTranslation()
  const { vehicleId = '' } = useParams()

  const { data, error, loading, reload } = useResource<PaymentDetails[]>(paymentsPath(vehicleId))

  const removal = useSubmission()
  const [editing, setEditing] = useState<string | null>(null)
  const [confirming, setConfirming] = useState<string | null>(null)

  const formatDate = dateFormatter(i18n.language)

  async function handleDelete(paymentId: string) {
    const failure = await removal.submit(async () => {
      await deletePayment(vehicleId, paymentId)
    })

    if (failure === null) {
      setConfirming(null)
      reload()
    }
  }

  return (
    <>
      <h1>{t('payments.title')}</h1>

      <p><Link to={paths.vehicle(vehicleId)}>{t('payments.backToVehicle')}</Link></p>

      <p>{t('payments.intro')}</p>

      {loading && <p role="status">{t('common.loading')}</p>}

      {error !== null && <p role="alert">{t(errorMessageKey(error.code))}</p>}

      {data !== null && data.length === 0 && <p>{t('payments.none')}</p>}

      {data !== null && data.length > 0 && (
        <ul>
          {data.map((payment) => {
            if (editing === payment.id) {
              return (
                <li key={payment.id}>
                  <PaymentForm
                    heading={t(`payments.kind.${payment.kind}`)}
                    submitLabel={t('payments.saveCorrection')}
                    initial={valuesOf(payment)}
                    onSave={async (body) => {
                      await correctPayment(vehicleId, payment.id, body)
                      setEditing(null)
                      reload()
                    }}
                    onCancel={() => { setEditing(null) }}
                  />
                </li>
              )
            }

            const next = payment.nextDueDate ?? null
            const due = next === null ? null : dueStateOf(payment.daysUntilNext ?? 0, next, formatDate)

            return (
              <li data-card key={payment.id}>
                <h2>{t(`payments.kind.${payment.kind}`)}</h2>

                <p data-subtitle>
                  {t(`payments.schedule.${countForm(payment.instalmentCount)}`, {
                    frequency: t(`payments.frequency.${payment.frequency}`),
                    count: payment.instalmentCount,
                    first: formatDate(payment.firstDueDate),
                    last: formatDate(payment.lastDueDate),
                  })}
                </p>

                {next === null || due === null
                  ? <p>{t('payments.finished')}</p>
                  : (
                    <>
                      <p>
                        {t('payments.next', {
                          date: formatDate(next),
                          number: payment.nextInstalment ?? 1,
                          count: payment.instalmentCount,
                        })}
                      </p>
                      <p data-tone={due.tone}>{t(due.key, due.values)}</p>
                    </>
                    )}

                <p>
                  {t('payments.reminders', {
                    leads: payment.remindDaysBefore
                      .map(lead => t(leadKey(lead)).toLocaleLowerCase(i18n.language))
                      .join(', '),
                  })}
                </p>

                {confirming === payment.id
                  ? (
                    <>
                      <p>{t('payments.confirmDelete')}</p>
                      <p data-actions>
                        <button
                          data-destructive
                          type="button"
                          onClick={() => { void handleDelete(payment.id) }}
                          disabled={removal.pending}
                        >
                          {t('payments.confirmDeleteYes')}
                        </button>
                        <button data-quiet type="button" onClick={() => { setConfirming(null) }}>
                          {t('payments.cancel')}
                        </button>
                      </p>
                    </>
                    )
                  : (
                    <p data-actions>
                      <button data-quiet type="button" onClick={() => { setEditing(payment.id) }}>
                        {t('payments.edit')}
                      </button>
                      <button data-quiet type="button" onClick={() => { setConfirming(payment.id) }}>
                        {t('payments.delete')}
                      </button>
                    </p>
                    )}
              </li>
            )
          })}
        </ul>
      )}

      <FormError error={removal.error} />

      <PaymentForm
        heading={t('payments.add')}
        submitLabel={t('payments.save')}
        initial={EMPTY_PAYMENT}
        onSave={async (body) => {
          await addPayment(vehicleId, body)
          reload()
        }}
      />
    </>
  )
}
