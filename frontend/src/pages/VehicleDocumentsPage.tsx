import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router'

import {
  addDocument, deleteDocument, documentTypes, documentsPath, type DocumentDetails,
} from '../api/endpoints/documents.ts'
import { useResource } from '../api/useResource.ts'
import { FormError } from '../components/FormError.tsx'
import { SelectField } from '../components/SelectField.tsx'
import { TextField } from '../components/TextField.tsx'
import { dateFormatter, stateOf } from '../documents/status.ts'
import { maxLength, required, type ValidationMessage } from '../forms/rules.ts'
import { useSubmission } from '../forms/useSubmission.ts'
import { validate } from '../forms/validate.ts'
import { errorMessageKey } from '../i18n/errorKey.ts'
import { paths } from '../routes/paths.ts'

type Field = 'type' | 'validFrom' | 'validUntil' | 'provider' | 'referenceNumber' | 'notes'

const rules = {
  validUntil: [required],
  provider: [maxLength(160)],
  referenceNumber: [maxLength(64)],
}

const EMPTY: Record<Field, string> = {
  type: 'RCA',
  validFrom: '',
  validUntil: '',
  provider: '',
  referenceNumber: '',
  notes: '',
}

/** An empty input means "not given", which the API expects as null rather than "". */
function orNull(value: string): string | null {
  const trimmed = value.trim()
  return trimmed === '' ? null : trimmed
}

/**
 * Screen 11 in specification section 5.
 *
 * <p>Correcting and renewing are screen 13 and are not here. This screen
 * configures: what documents exist, and adding or removing one.
 *
 * <p>Deletion confirms in place rather than through window.confirm, as the
 * vehicle screen does - that dialog is written in the browser's language, not
 * the application's.
 */
export function VehicleDocumentsPage() {
  const { t, i18n } = useTranslation()
  const { vehicleId = '' } = useParams()

  const { data, error, loading, reload } = useResource<DocumentDetails[]>(documentsPath(vehicleId))

  const creation = useSubmission()
  const removal = useSubmission()

  const [form, setForm] = useState<Record<Field, string>>(EMPTY)
  const [messages, setMessages] = useState<Partial<Record<Field, ValidationMessage>>>({})
  const [confirming, setConfirming] = useState<string | null>(null)

  const formatDate = dateFormatter(i18n.language)

  function set(field: Field, value: string) {
    setForm(previous => ({ ...previous, [field]: value }))
  }

  async function handleAdd(event: FormEvent) {
    event.preventDefault()

    const broken = validate(form, rules)
    setMessages(broken)

    if (Object.keys(broken).length > 0) {
      return
    }

    const failure = await creation.submit(async () => {
      await addDocument(vehicleId, {
        type: form.type,
        validFrom: orNull(form.validFrom),
        validUntil: form.validUntil,
        provider: orNull(form.provider),
        referenceNumber: orNull(form.referenceNumber),
        notes: orNull(form.notes),
      })
    })

    if (failure === null) {
      setForm(EMPTY)
      reload()
    }
  }

  async function handleDelete(documentId: string) {
    const failure = await removal.submit(async () => {
      await deleteDocument(vehicleId, documentId)
    })

    if (failure === null) {
      setConfirming(null)
      reload()
    }
  }

  return (
    <>
      <h1>{t('documents.title')}</h1>

      <p><Link to={paths.vehicle(vehicleId)}>{t('documents.backToVehicle')}</Link></p>

      {loading && <p role="status">{t('common.loading')}</p>}

      {error !== null && <p role="alert">{t(errorMessageKey(error.code))}</p>}

      {data !== null && data.length === 0 && <p>{t('documents.none')}</p>}

      {data !== null && data.length > 0 && (
        <ul>
          {data.map((document) => {
            const state = stateOf(document, formatDate)

            return (
              <li key={document.id} data-tone={state.tone}>
                <h2>{t(`documents.type.${document.type}`)}</h2>

                <p>
                  {document.validFrom == null
                    ? t('documents.period', { until: formatDate(document.validUntil) })
                    : t('documents.periodFrom', {
                        from: formatDate(document.validFrom),
                        until: formatDate(document.validUntil),
                      })}
                </p>

                <p>{t(state.key, state.values)}</p>

                {document.provider != null && <p>{document.provider}</p>}
                {document.referenceNumber != null && <p>{document.referenceNumber}</p>}
                {document.notes != null && <p>{document.notes}</p>}

                {confirming === document.id
                  ? (
                    <>
                      <p>{t('documents.confirmDelete')}</p>
                      <button
                        type="button"
                        onClick={() => { void handleDelete(document.id) }}
                        disabled={removal.pending}
                      >
                        {t('documents.confirmDeleteYes')}
                      </button>
                      <button type="button" onClick={() => { setConfirming(null) }}>
                        {t('documents.cancel')}
                      </button>
                    </>
                    )
                  : (
                    <button type="button" onClick={() => { setConfirming(document.id) }}>
                      {t('documents.delete')}
                    </button>
                    )}
              </li>
            )
          })}
        </ul>
      )}

      <FormError error={removal.error} />

      <h2>{t('documents.add')}</h2>

      <form onSubmit={(event) => { void handleAdd(event) }} noValidate>
        <FormError error={creation.error} />

        <SelectField
          label={t('documents.fields.type')}
          value={form.type}
          options={documentTypes.map(type => ({
            value: type,
            label: t(`documents.type.${type}`),
          }))}
          onChange={(value) => { set('type', value) }}
        />

        <TextField
          label={t('documents.fields.validFrom')}
          type="date"
          value={form.validFrom}
          onChange={(value) => { set('validFrom', value) }}
          message={messages.validFrom}
        />

        <TextField
          label={t('documents.fields.validUntil')}
          type="date"
          value={form.validUntil}
          onChange={(value) => { set('validUntil', value) }}
          message={messages.validUntil}
        />

        <TextField
          label={t('documents.fields.provider')}
          value={form.provider}
          maxLength={160}
          onChange={(value) => { set('provider', value) }}
          message={messages.provider}
        />

        <TextField
          label={t('documents.fields.referenceNumber')}
          value={form.referenceNumber}
          maxLength={64}
          onChange={(value) => { set('referenceNumber', value) }}
          message={messages.referenceNumber}
        />

        <TextField
          label={t('documents.fields.notes')}
          multiline
          value={form.notes}
          onChange={(value) => { set('notes', value) }}
          message={messages.notes}
        />

        <button type="submit" disabled={creation.pending}>{t('documents.save')}</button>
      </form>
    </>
  )
}