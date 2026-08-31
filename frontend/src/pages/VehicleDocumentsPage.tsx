import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router'

import {
  addDocument, deleteDocument, documentTypes, documentsPath, type DocumentDetails,
} from '../api/endpoints/documents.ts'
import { useResource } from '../api/useResource.ts'
import { FormError } from '../components/FormError.tsx'
import { SelectField } from '../components/SelectField.tsx'
import { DocumentFields } from '../documents/DocumentFields.tsx'
import {
  EMPTY_PERIOD, periodBody, periodRules, type PeriodField, type PeriodValues,
} from '../documents/fields.ts'
import { dateFormatter, stateOf } from '../documents/status.ts'
import type { ValidationMessage } from '../forms/rules.ts'
import { useSubmission } from '../forms/useSubmission.ts'
import { validate } from '../forms/validate.ts'
import { errorMessageKey } from '../i18n/errorKey.ts'
import { paths } from '../routes/paths.ts'

/**
 * Screen 11 in specification section 5.
 *
 * <p>This screen configures: what documents exist for a vehicle, and adding or
 * removing one. Correcting and renewing are screen 13, reached from each entry -
 * they operate on a single record and section 5 gives them a screen of their
 * own.
 *
 * <p>The five period inputs come from DocumentFields rather than being written
 * here, because three forms need the same ones and the backend makes the same
 * separation with DocumentPeriod. What differs between adding, correcting and
 * renewing is the type: this form has it, a renewal does not.
 *
 * <p>Deletion confirms in place rather than through window.confirm, as the
 * vehicle screen does - that dialog is written in the browser's language, not
 * the application's, so it would put untranslated text beside translated text.
 */
export function VehicleDocumentsPage() {
  const { t, i18n } = useTranslation()
  const { vehicleId = '' } = useParams()

  const { data, error, loading, reload } = useResource<DocumentDetails[]>(documentsPath(vehicleId))

  const creation = useSubmission()
  const removal = useSubmission()

  const [type, setType] = useState('RCA')
  const [values, setValues] = useState<PeriodValues>(EMPTY_PERIOD)
  const [messages, setMessages] = useState<Partial<Record<PeriodField, ValidationMessage>>>({})
  const [confirming, setConfirming] = useState<string | null>(null)

  const formatDate = dateFormatter(i18n.language)

  async function handleAdd(event: FormEvent) {
    event.preventDefault()

    const broken = validate(values, periodRules)
    setMessages(broken)

    if (Object.keys(broken).length > 0) {
      return
    }

    const failure = await creation.submit(async () => {
      await addDocument(vehicleId, { type, ...periodBody(values) })
    })

    if (failure === null) {
      setValues(EMPTY_PERIOD)
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

      <p data-actions>
        <Link data-action="secondary" to={paths.history(vehicleId)}>
          {t('history.open')}
        </Link>
      </p>

      {loading && <p role="status">{t('common.loading')}</p>}

      {error !== null && <p role="alert">{t(errorMessageKey(error.code))}</p>}

      {data !== null && data.length === 0 && <p>{t('documents.none')}</p>}

      {data !== null && data.length > 0 && (
        <ul>
          {data.map((document) => {
            const state = stateOf(document, formatDate)

            // The tone moved off the item and onto the sentence it describes,
            // which is where VehicleDocumentDetailsPage already had it. On the
            // dashboard a toned item is one line; here it is a whole block, and
            // colouring all of it made a document with an expired date read as
            // an error rather than as a record.
            return (
              <li data-card key={document.id}>
                <h2>
                  <Link to={paths.document(vehicleId, document.id)}>
                    {t(`documents.type.${document.type}`)}
                  </Link>
                </h2>

                <p data-subtitle>
                  {document.validFrom == null
                    ? t('documents.period', { until: formatDate(document.validUntil) })
                    : t('documents.periodFrom', {
                        from: formatDate(document.validFrom),
                        until: formatDate(document.validUntil),
                      })}
                </p>

                <p data-tone={state.tone}>{t(state.key, state.values)}</p>

                {/*
                  Labelled, because a bare string tells nobody what it is: an
                  insurer's name, a policy number and a note all looked like
                  three anonymous lines under the date.
                */}
                {(document.provider != null
                  || document.referenceNumber != null
                  || document.notes != null) && (
                  <dl>
                    {document.provider != null && (
                      <>
                        <dt>{t('documents.fields.provider')}</dt>
                        <dd>{document.provider}</dd>
                      </>
                    )}
                    {document.referenceNumber != null && (
                      <>
                        <dt>{t('documents.fields.referenceNumber')}</dt>
                        <dd>{document.referenceNumber}</dd>
                      </>
                    )}
                    {document.notes != null && (
                      <>
                        <dt>{t('documents.fields.notes')}</dt>
                        <dd>{document.notes}</dd>
                      </>
                    )}
                  </dl>
                )}

                {confirming === document.id
                  ? (
                    <>
                      <p>{t('documents.confirmDelete')}</p>
                      <p data-actions>
                        <button
                          data-destructive
                          type="button"
                          onClick={() => { void handleDelete(document.id) }}
                          disabled={removal.pending}
                        >
                          {t('documents.confirmDeleteYes')}
                        </button>
                        <button data-quiet type="button" onClick={() => { setConfirming(null) }}>
                          {t('documents.cancel')}
                        </button>
                      </p>
                    </>
                    )
                  : (
                    <button data-quiet type="button" onClick={() => { setConfirming(document.id) }}>
                      {t('documents.delete')}
                    </button>
                    )}
              </li>
            )
          })}
        </ul>
      )}

      <FormError error={removal.error} />

      <form data-card onSubmit={(event) => { void handleAdd(event) }} noValidate>
        <h2>{t('documents.add')}</h2>

        <FormError error={creation.error} />

        <SelectField
          label={t('documents.fields.type')}
          value={type}
          options={documentTypes.map(one => ({
            value: one,
            label: t(`documents.type.${one}`),
          }))}
          onChange={(value) => { setType(value) }}
        />

        <DocumentFields
          values={values}
          messages={messages}
          onChange={(field, value) => {
            setValues(previous => ({ ...previous, [field]: value }))
          }}
        />

        <button type="submit" disabled={creation.pending}>{t('documents.save')}</button>
      </form>
    </>
  )
}