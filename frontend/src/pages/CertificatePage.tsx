import { useId, useRef, useState, type ChangeEvent, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router'

import { certificatePath, saveCertificate, type CertificateData } from '../api/endpoints/certificate.ts'
import { scanCertificate, type OcrScan } from '../api/endpoints/ocr.ts'
import { useResource } from '../api/useResource.ts'
import { CertificateOverlay } from '../certificate/CertificateOverlay.tsx'
import { certificateFields, fieldGroups, fieldsOf, type CertificateField } from '../certificate/fields.ts'
import { applyScan, tally, type FieldStatuses } from '../certificate/scan.ts'
import { fromForm, toForm, type CertificateForm } from '../certificate/values.ts'
import { FormError } from '../components/FormError.tsx'
import { maxLength, required, type Rule } from '../forms/rules.ts'
import { useSubmission } from '../forms/useSubmission.ts'
import { fieldMessagesFrom, validate, type FieldMessages, type FieldRules } from '../forms/validate.ts'
import { errorMessageKey } from '../i18n/errorKey.ts'
import { paths } from '../routes/paths.ts'

/**
 * Built from the field table rather than written out thirty-two times. The table
 * already carries the backend's column widths and which four fields section 8
 * makes mandatory, so a bound lives in one place on this side too.
 */
const rules: FieldRules<CertificateField> = Object.fromEntries(
  certificateFields.map((field): [CertificateField, Rule[]] => {
    const applicable: Rule[] = []
    if (field.required === true) {
      applicable.push(required)
    }
    if (field.maxLength !== undefined) {
      applicable.push(maxLength(field.maxLength))
    }
    return [field.name, applicable]
  }),
) as FieldRules<CertificateField>

/**
 * Screen 10 in specification section 5: the approved template with editable
 * fields laid over it, which is the UI model section 7 fixes. The same fields
 * are filled either by hand or from a photograph - section 13's OCR is a way of
 * proposing values here, never a second screen and never a second shape.
 *
 * <p>Saving sends every field, which is what makes the backend's replacement
 * semantics safe: a field omitted there is cleared, and this never omits one.
 *
 * <p><strong>A scan proposes; it does not save.</strong> Section 13 requires the
 * person to review and correct first, so the scan only changes what is on
 * screen, and the same Save button they would have used anyway is what stores
 * it.
 *
 * <p>Problems are listed above the template rather than beside each box. There
 * is no room next to a field on a scanned document, and a list that names the
 * field and says what is wrong is readable where a floating marker would not be.
 * Each field still carries aria-invalid and its own description.
 */
export function CertificatePage() {
  const { t } = useTranslation()
  const { vehicleId = '' } = useParams()
  const photoInputId = useId()

  const { data, error, loading } = useResource<CertificateData>(certificatePath(vehicleId))
  const save = useSubmission()
  const scan = useSubmission()

  const [draft, setDraft] = useState<CertificateForm | null>(null)
  const [messages, setMessages] = useState<FieldMessages<CertificateField>>({})
  const [statuses, setStatuses] = useState<FieldStatuses>({})
  const [saved, setSaved] = useState(false)

  /**
   * Where the saved certificate is caught. useSubmission returns the failure and
   * not the value, and a plain local cannot carry one out of the callback -
   * TypeScript does not follow assignments made inside a closure.
   */
  const stored = useRef<CertificateData | null>(null)

  /** The same problem again, for the scan's answer. */
  const proposals = useRef<OcrScan | null>(null)

  const form = draft ?? (data === null ? null : toForm(data))
  const counts = tally(statuses)
  const scanned = counts.detected + counts.needsReview + counts.notDetected > 0

  const problems = fieldGroups.flatMap(group => fieldsOf(group).flatMap(spec => {
    const message = messages[spec.name]
    return message === undefined
      ? []
      : [{
          name: spec.name,
          label: t(`certificate.fields.${spec.name}`),
          text: t(message.key, message.values),
        }]
  }))

  function change(field: CertificateField, value: string) {
    if (form === null) {
      return
    }
    setSaved(false)
    setDraft({ ...form, [field]: value })
  }

  async function handlePhotograph(event: ChangeEvent<HTMLInputElement>) {
    const input = event.target
    const file = input.files?.[0]

    // Clearing the input is what lets the same photograph be tried a second
    // time: a file input fires no change event when the selection has not
    // changed, so without this a retry after a failed scan would do nothing.
    input.value = ''

    if (file === undefined || form === null) {
      return
    }

    const failure = await scan.submit(async () => {
      proposals.current = await scanCertificate(file)
    })

    if (failure !== null || proposals.current === null) {
      return
    }

    const result = applyScan(form, proposals.current)

    setDraft(result.form)
    setStatuses(result.statuses)
    // The values underneath them changed. Problems reported against the old ones
    // would be pointing at text that is no longer on screen.
    setMessages({})
    setSaved(false)
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()

    if (form === null) {
      return
    }

    const broken = validate(form, rules)
    setMessages(broken)

    if (Object.keys(broken).length > 0) {
      return
    }

    const failure = await save.submit(async () => {
      stored.current = await saveCertificate(vehicleId, fromForm(form))
    })

    if (failure === null && stored.current !== null) {
      // The answer, not another question. Reloading instead would empty the
      // resource while the request was in flight, and the whole template would
      // vanish and come back on every save.
      setDraft(toForm(stored.current))
      setSaved(true)
      return
    }

    if (failure !== null && failure.fieldErrors.length > 0) {
      setMessages(fieldMessagesFrom(failure, form, rules))
    }
  }

  return (
    <>
      <h1>{t('screens.certificate')}</h1>

      <p><Link to={paths.vehicle(vehicleId)}>{t('certificate.backToVehicle')}</Link></p>

      {loading && <p role="status">{t('common.loading')}</p>}

      {error !== null && <p role="alert">{t(errorMessageKey(error.code))}</p>}

      {form !== null && (
        <form onSubmit={(event) => { void handleSubmit(event) }} noValidate>
          <FormError error={save.error} />

          <p>{t('certificate.sensitiveNote')}</p>

          <div>
            <label htmlFor={photoInputId}>{t('certificate.scan.choose')}</label>
            <input
              id={photoInputId}
              type="file"
              accept="image/jpeg,image/png"
              onChange={(event) => { void handlePhotograph(event) }}
              disabled={scan.pending}
            />

            {scan.pending && <p role="status">{t('certificate.scan.pending')}</p>}

            <FormError error={scan.error} />

            {scanned && (
              <>
                <p role="status">{t('certificate.scan.result', counts)}</p>
                <p>{t('certificate.scan.note')}</p>
              </>
            )}
          </div>

          {problems.length > 0 && (
            <div role="alert">
              <p>{t('certificate.problems')}</p>
              <ul>
                {problems.map(problem => (
                  <li key={problem.name}>{problem.label}: {problem.text}</li>
                ))}
              </ul>
            </div>
          )}

          <CertificateOverlay form={form} messages={messages} statuses={statuses} onChange={change} />

          <button type="submit" disabled={save.pending}>{t('certificate.save')}</button>

          {saved && <p role="status">{t('certificate.saved')}</p>}
        </form>
      )}
    </>
  )
}