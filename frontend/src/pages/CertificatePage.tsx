import { useRef, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router'

import { certificatePath, saveCertificate, type CertificateData } from '../api/endpoints/certificate.ts'
import { useResource } from '../api/useResource.ts'
import {
  certificateFields, fieldGroups, fieldsOf, type CertificateField, type FieldSpec,
} from '../certificate/fields.ts'
import { fromForm, toForm, type CertificateForm } from '../certificate/values.ts'
import { CheckboxField } from '../components/CheckboxField.tsx'
import { FormError } from '../components/FormError.tsx'
import { TextField } from '../components/TextField.tsx'
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
 * Screen 10 in specification section 5, in its manual form. Phase 8.3 lays these
 * same fields over the template image at calibrated coordinates, and Phase 9
 * fills them from OCR - which is why the screen is generated from the field
 * table rather than hand-written: the overlay attaches coordinates to entries
 * that already exist instead of keeping a second list that can drift.
 *
 * <p>Saving sends every field, which is what makes the backend's replacement
 * semantics safe: a field omitted there is cleared, and this never omits one.
 */
export function CertificatePage() {
  const { t } = useTranslation()
  const { vehicleId = '' } = useParams()

  const { data, error, loading } = useResource<CertificateData>(certificatePath(vehicleId))
  const save = useSubmission()

  // Null means "showing whatever the server last said". Copying server state
  // into state with an effect would overwrite whatever somebody was typing the
  // moment a response landed.
  const [draft, setDraft] = useState<CertificateForm | null>(null)
  const [messages, setMessages] = useState<FieldMessages<CertificateField>>({})
  const [saved, setSaved] = useState(false)

  /**
   * Where the saved certificate is caught. useSubmission returns the failure and
   * not the value, and a plain local cannot carry one out of the callback -
   * TypeScript does not follow assignments made inside a closure.
   */
  const stored = useRef<CertificateData | null>(null)

  const form = draft ?? (data === null ? null : toForm(data))

  function change(field: CertificateField, value: string) {
    if (form === null) {
      return
    }
    setSaved(false)
    setDraft({ ...form, [field]: value })
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
      // resource while the request was in flight, and this whole form would
      // vanish and come back on every save.
      setDraft(toForm(stored.current))
      setSaved(true)
      return
    }

    if (failure !== null && failure.fieldErrors.length > 0) {
      setMessages(fieldMessagesFrom(failure, form, rules))
    }
  }

  function field(spec: FieldSpec) {
    if (form === null) {
      return null
    }

    const label = t(`certificate.fields.${spec.name}`)

    if (spec.kind === 'boolean') {
      return (
        <CheckboxField
          key={spec.name}
          label={label}
          checked={form[spec.name] === 'true'}
          onChange={(checked) => { change(spec.name, checked ? 'true' : '') }}
        />
      )
    }

    return (
      <TextField
        key={spec.name}
        label={label}
        type={spec.kind === 'date' ? 'date' : 'text'}
        // Numbers are text inputs on purpose. A number input in a Romanian
        // browser refuses "66,5", which is how the power of a car is written
        // here; the value is parsed on the way out instead.
        inputMode={spec.kind === 'integer' || spec.kind === 'decimal' ? 'numeric' : undefined}
        multiline={spec.kind === 'multiline'}
        maxLength={spec.maxLength}
        value={form[spec.name]}
        onChange={(value) => { change(spec.name, value) }}
        message={messages[spec.name]}
      />
    )
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

          {fieldGroups.map(group => (
            <fieldset key={group}>
              <legend>{t(`certificate.groups.${group}`)}</legend>
              {group === 'owner' && <p>{t('certificate.sensitiveNote')}</p>}
              {fieldsOf(group).map(spec => field(spec))}
            </fieldset>
          ))}

          <button type="submit" disabled={save.pending}>{t('certificate.save')}</button>

          {saved && <p role="status">{t('certificate.saved')}</p>}
        </form>
      )}
    </>
  )
}