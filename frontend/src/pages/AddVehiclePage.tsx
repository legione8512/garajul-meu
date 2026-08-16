import { useId, useRef, useState, type ChangeEvent, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'

import { scanCertificate, type OcrScan, type ScanStatus } from '../api/endpoints/ocr.ts'
import { createVehicle } from '../api/endpoints/vehicles.ts'
import { proposalsFor, tally } from '../certificate/scan.ts'
import { FormError } from '../components/FormError.tsx'
import { TextField } from '../components/TextField.tsx'
import { maxLength, required } from '../forms/rules.ts'
import { useSubmission } from '../forms/useSubmission.ts'
import { fieldMessagesFrom, validate, type FieldMessages } from '../forms/validate.ts'
import { paths } from '../routes/paths.ts'

type Field = 'registrationNumber' | 'make' | 'commercialDescription' | 'vin' | 'displayName'

/** The same bounds CreateVehicleRequest declares on the backend. */
const rules = {
  registrationNumber: [required, maxLength(32)],
  make: [required, maxLength(64)],
  commercialDescription: [required, maxLength(128)],
  vin: [required, maxLength(32)],
  displayName: [maxLength(120)],
}

const EMPTY: Record<Field, string> = {
  registrationNumber: '',
  make: '',
  commercialDescription: '',
  vin: '',
  displayName: '',
}

/**
 * The four this form holds that are also printed on the certificate. The
 * nickname is not on the document at all - it is what its owner calls the car -
 * so no scan can propose one, and a scan must not touch one already typed.
 */
const SCANNABLE: readonly Field[] = ['registrationNumber', 'make', 'commercialDescription', 'vin']

/**
 * Screen 8 in specification section 5.
 *
 * <p>Four required fields and a nickname. Section 8 names exactly those four as
 * the minimum to save a vehicle, and they are exactly the four columns section
 * 10.3 declares NOT NULL - the requirement is stated twice and agrees with
 * itself.
 *
 * <p><strong>Section 3's two entrances, both here.</strong> It calls creation
 * "manually or from a photographed Romanian registration certificate", and until
 * now only the manual one existed: the certificate screen can scan, but reaching
 * it requires having already typed the VIN and the make, which is the wrong way
 * round for somebody holding the document. The photograph fills the same form
 * the hand does; nothing is created until the same button is pressed.
 *
 * <p>Per-field scan states are deliberately not drawn here. The overlay needs
 * them because it has thirty-three boxes over an image; this form has four
 * inputs in a column, where what is missing is not decoration but the name of
 * the field to look at - so the uncertain ones are listed by name instead.
 *
 * <p>No rule checks the VIN's seventeen characters. Section 13 places semantic
 * VIN validation inside the OCR review, and refusing a short one typed by hand
 * would lock out the vehicles that legitimately have one.
 */
export function AddVehiclePage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const { pending, error, submit } = useSubmission()
  const scan = useSubmission()
  const photoInputId = useId()

  const [values, setValues] = useState<Record<Field, string>>(EMPTY)
  const [messages, setMessages] = useState<FieldMessages<Field>>({})
  const [statuses, setStatuses] = useState<Partial<Record<Field, ScanStatus>>>({})

  /**
   * Where the new vehicle's identifier is caught. useSubmission returns the
   * failure, not the value, so the identifier has to come out of the callback -
   * and a plain local cannot carry it: TypeScript does not follow assignments
   * made inside a closure, so it would narrow the variable to null and refuse
   * the navigation. A ref is the one thing control-flow analysis leaves alone
   * across a call.
   */
  const createdId = useRef<string | null>(null)

  /** The same problem again, for the scan's answer. */
  const proposals = useRef<OcrScan | null>(null)

  const counts = tally(statuses)
  const scanned = counts.detected + counts.needsReview + counts.notDetected > 0
  const uncertain = SCANNABLE.filter(field => statuses[field] === 'NEEDS_REVIEW')

  async function handlePhotograph(event: ChangeEvent<HTMLInputElement>) {
    const input = event.target
    const file = input.files?.[0]

    // Clearing the input is what lets the same photograph be tried a second
    // time: a file input fires no change event when the selection has not
    // changed, so without this a retry after a failed scan would do nothing.
    input.value = ''

    if (file === undefined) {
      return
    }

    const failure = await scan.submit(async () => {
      proposals.current = await scanCertificate(file)
    })

    if (failure !== null || proposals.current === null) {
      return
    }

    const result = proposalsFor(values, proposals.current, SCANNABLE)

    setValues(result.values)
    setStatuses(result.statuses)
    // The values underneath them changed. Problems reported against the old ones
    // would be pointing at text that is no longer in the inputs.
    setMessages({})
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()

    const broken = validate(values, rules)
    setMessages(broken)

    if (Object.keys(broken).length > 0) {
      return
    }

    const failure = await submit(async () => {
      // The nickname goes as typed, blank included. The backend already turns a
      // blank one into nothing stored, and duplicating that decision here would
      // put the definition of "no nickname" in two places.
      createdId.current = (await createVehicle(values)).id
    })

    if (failure === null && createdId.current !== null) {
      void navigate(paths.vehicle(createdId.current))
      return
    }

    if (failure !== null && failure.fieldErrors.length > 0) {
      setMessages(fieldMessagesFrom(failure, values, rules))
    }
  }

  return (
    <>
      <h1>{t('screens.addVehicle')}</h1>
      <p>{t('addVehicle.instructions')}</p>

      <form onSubmit={(event) => { void handleSubmit(event) }} noValidate>
        <FormError error={error} />

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
              {uncertain.length > 0 && (
                <p>{t('addVehicle.scan.review', {
                  fields: uncertain.map(field => t(`fields.${field}`)).join(', '),
                })}
                </p>
              )}
              <p>{t('certificate.scan.note')}</p>
            </>
          )}
        </div>

        <TextField
          label={t('fields.registrationNumber')}
          value={values.registrationNumber}
          onChange={(registrationNumber) => { setValues({ ...values, registrationNumber }) }}
          message={messages.registrationNumber}
        />

        <TextField
          label={t('fields.make')}
          value={values.make}
          onChange={(make) => { setValues({ ...values, make }) }}
          message={messages.make}
        />

        <TextField
          label={t('fields.commercialDescription')}
          value={values.commercialDescription}
          onChange={(commercialDescription) => { setValues({ ...values, commercialDescription }) }}
          message={messages.commercialDescription}
        />

        <TextField
          label={t('fields.vin')}
          value={values.vin}
          onChange={(vin) => { setValues({ ...values, vin }) }}
          message={messages.vin}
        />

        <TextField
          label={t('fields.displayName')}
          value={values.displayName}
          onChange={(displayName) => { setValues({ ...values, displayName }) }}
          message={messages.displayName}
        />

        <button type="submit" disabled={pending}>{t('addVehicle.submit')}</button>
      </form>
    </>
  )
}