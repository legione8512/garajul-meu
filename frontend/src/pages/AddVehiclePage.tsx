import { useRef, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'

import { createVehicle } from '../api/endpoints/vehicles.ts'
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
 * Screen 8 in specification section 5.
 *
 * <p>Four required fields and a nickname. Section 8 names exactly those four as
 * the minimum to save a vehicle, and they are exactly the four columns section
 * 10.3 declares NOT NULL - the requirement is stated twice and agrees with
 * itself. The remaining thirty certificate fields arrive in Phase 8 and the
 * photographed path in Phase 9; section 3 lists manual creation beside it as an
 * equal, so this screen is finished rather than provisional.
 *
 * <p>No rule checks the VIN's seventeen characters. Section 13 places semantic
 * VIN validation inside the OCR review, and refusing a short one typed by hand
 * would lock out the vehicles that legitimately have one.
 */
export function AddVehiclePage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const { pending, error, submit } = useSubmission()

  const [values, setValues] = useState<Record<Field, string>>(EMPTY)
  const [messages, setMessages] = useState<FieldMessages<Field>>({})

  /**
   * Where the new vehicle's identifier is caught. useSubmission returns the
   * failure, not the value, so the identifier has to come out of the callback -
   * and a plain local cannot carry it: TypeScript does not follow assignments
   * made inside a closure, so it would narrow the variable to null and refuse
   * the navigation. A ref is the one thing control-flow analysis leaves alone
   * across a call.
   */
  const createdId = useRef<string | null>(null)

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