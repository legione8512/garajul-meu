import { useRef, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate, useParams } from 'react-router'

import {
  correctDocument, documentPath, documentTypes, renewDocument, type DocumentDetails,
} from '../api/endpoints/documents.ts'
import { useResource } from '../api/useResource.ts'
import { FormError } from '../components/FormError.tsx'
import { SelectField } from '../components/SelectField.tsx'
import { DocumentFields } from '../documents/DocumentFields.tsx'
import { DocumentReminders } from '../documents/DocumentReminders.tsx'
import {
  EMPTY_PERIOD, periodBody, periodOf, periodRules,
  type PeriodField, type PeriodValues,
} from '../documents/fields.ts'
import { dateFormatter, stateOf } from '../documents/status.ts'
import type { ValidationMessage } from '../forms/rules.ts'
import { useSubmission } from '../forms/useSubmission.ts'
import { validate } from '../forms/validate.ts'
import { errorMessageKey } from '../i18n/errorKey.ts'
import { paths } from '../routes/paths.ts'

type Messages = Partial<Record<PeriodField, ValidationMessage>>

/**
 * Screen 13 in specification section 5.
 *
 * <p><strong>Keyed on the document.</strong> React Router keeps a component
 * mounted when only a path parameter changes, so without this the person lands
 * on the record a renewal just created with the renewal form still holding what
 * created it, and with the previous document's corrections still in state. The
 * key forces a remount, which is the whole of the fix - and it now carries the
 * reminder list with it, which reads a different address per document and would
 * otherwise show the superseded record's schedule.
 */
export function VehicleDocumentDetailsPage() {
  const { documentId = '' } = useParams()

  return <DocumentScreen key={documentId} />
}

/**
 * <p>Two forms, not one with a mode, because they do genuinely different things.
 * Correcting **replaces this record** - an optional field left empty is cleared,
 * which is the only way a note can be removed. Renewing **creates a new one**,
 * leaves this one exactly as it is, and takes nothing from it but the type.
 *
 * <p>After a renewal the screen moves to the new document. Staying would show
 * the superseded record as though it were the current one, which is the mistake
 * section 11 spends a paragraph forbidding on the dashboard.
 *
 * <p>The correction form derives its values from the server's answer rather than
 * copying them into state with an effect - the pattern the vehicle screen uses,
 * for the same reason.
 *
 * <p>Both results are held in refs rather than plain locals. TypeScript's
 * control-flow analysis cannot see an assignment made inside the async callback,
 * so a local narrows to `never` after the null check - which fails outright when
 * a property is read from it, and, worse, passes silently when one is not.
 *
 * <p>The reminders sit between the state and the forms, and that is where they
 * belong: the sentence above says what is true today, the list says what will be
 * said about it later, and both are things to read before either form is a thing
 * to do.
 */
function DocumentScreen() {
  const { t, i18n } = useTranslation()
  const navigate = useNavigate()
  const { vehicleId = '', documentId = '' } = useParams()

  const { data, error, loading } = useResource<DocumentDetails>(documentPath(vehicleId, documentId))

  const correction = useSubmission()
  const renewal = useSubmission()

  const [corrected, setCorrected] = useState<DocumentDetails | null>(null)
  const [draft, setDraft] = useState<PeriodValues | null>(null)
  const [type, setType] = useState<string | null>(null)
  const [correctionMessages, setCorrectionMessages] = useState<Messages>({})

  const [renewalValues, setRenewalValues] = useState<PeriodValues>(EMPTY_PERIOD)
  const [renewalMessages, setRenewalMessages] = useState<Messages>({})

  const fresh = useRef<DocumentDetails | null>(null)
  const created = useRef<DocumentDetails | null>(null)

  const document = corrected ?? data
  const values = draft ?? (document === null ? EMPTY_PERIOD : periodOf(document))
  const chosenType = type ?? document?.type ?? 'RCA'
  const formatDate = dateFormatter(i18n.language)

  function editCorrection(field: PeriodField, value: string) {
    setDraft({ ...values, [field]: value })
  }

  function editRenewal(field: PeriodField, value: string) {
    setRenewalValues(previous => ({ ...previous, [field]: value }))
  }

  async function handleCorrect(event: FormEvent) {
    event.preventDefault()

    const broken = validate(values, periodRules)
    setCorrectionMessages(broken)

    if (Object.keys(broken).length > 0) {
      return
    }

    const failure = await correction.submit(async () => {
      fresh.current = await correctDocument(vehicleId, documentId, {
        type: chosenType,
        ...periodBody(values),
      })
    })

    if (failure === null && fresh.current !== null) {
      setCorrected(fresh.current)
      setDraft(null)
      setType(null)
    }
  }

  async function handleRenew(event: FormEvent) {
    event.preventDefault()

    const broken = validate(renewalValues, periodRules)
    setRenewalMessages(broken)

    if (Object.keys(broken).length > 0) {
      return
    }

    const failure = await renewal.submit(async () => {
      created.current = await renewDocument(vehicleId, documentId, periodBody(renewalValues))
    })

    if (failure === null && created.current !== null) {
      void navigate(paths.document(vehicleId, created.current.id))
    }
  }

  return (
    <>
      <h1>
        {document === null
          ? t('documents.detailsTitle')
          : t(`documents.type.${document.type}`)}
      </h1>

      <p><Link to={paths.documents(vehicleId)}>{t('documents.backToList')}</Link></p>

      {loading && <p role="status">{t('common.loading')}</p>}

      {error !== null && <p role="alert">{t(errorMessageKey(error.code))}</p>}

      {document !== null && (
        <>
          <p>
            {document.validFrom == null
              ? t('documents.period', { until: formatDate(document.validUntil) })
              : t('documents.periodFrom', {
                  from: formatDate(document.validFrom),
                  until: formatDate(document.validUntil),
                })}
          </p>

          {(() => {
            const state = stateOf(document, formatDate)
            return <p data-tone={state.tone}>{t(state.key, state.values)}</p>
          })()}

          <DocumentReminders vehicleId={vehicleId} documentId={documentId} />

          <h2>{t('documents.correct')}</h2>

          <form onSubmit={(event) => { void handleCorrect(event) }} noValidate>
            <FormError error={correction.error} />

            <SelectField
              label={t('documents.fields.type')}
              value={chosenType}
              options={documentTypes.map(one => ({
                value: one,
                label: t(`documents.type.${one}`),
              }))}
              onChange={(value) => { setType(value) }}
            />

            <DocumentFields
              values={values}
              messages={correctionMessages}
              onChange={editCorrection}
            />

            <button type="submit" disabled={correction.pending}>
              {t('documents.saveCorrection')}
            </button>
          </form>

          <h2>{t('documents.renew')}</h2>
          <p>{t('documents.renewNote')}</p>

          <form onSubmit={(event) => { void handleRenew(event) }} noValidate>
            <FormError error={renewal.error} />

            <DocumentFields
              values={renewalValues}
              messages={renewalMessages}
              onChange={editRenewal}
            />

            <button type="submit" disabled={renewal.pending}>
              {t('documents.saveRenewal')}
            </button>
          </form>
        </>
      )}
    </>
  )
}