import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate, useParams } from 'react-router'

import {
  deleteVehicle, renameVehicle, vehicleLabel, vehiclePath, type VehicleDetails,
} from '../api/endpoints/vehicles.ts'
import { useResource } from '../api/useResource.ts'
import { FormError } from '../components/FormError.tsx'
import { TextField } from '../components/TextField.tsx'
import { maxLength, type ValidationMessage } from '../forms/rules.ts'
import { useSubmission } from '../forms/useSubmission.ts'
import { validate } from '../forms/validate.ts'
import { errorMessageKey } from '../i18n/errorKey.ts'
import { paths } from '../routes/paths.ts'

const nicknameRules = { displayName: [maxLength(120)] }

/**
 * Screen 12 in specification section 5.
 *
 * <p>The heading is the vehicle's own label rather than a fixed screen title,
 * which is the point of a detail screen. It falls back to the generic title
 * while there is nothing to name yet - including when the vehicle is not there
 * at all, which is what somebody following a stale link sees.
 *
 * <p>Deletion confirms in place rather than through window.confirm. That dialog
 * is written in the browser's language, not the application's, so it would put
 * untranslated text beside translated text on the same screen - the same reason
 * every form here carries noValidate.
 */
export function VehicleDetailsPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const { vehicleId = '' } = useParams()

  const { data, error, loading, reload } = useResource<VehicleDetails>(vehiclePath(vehicleId))

  const rename = useSubmission()
  const removal = useSubmission()

  // Null means "showing whatever the server last said". Deriving the input's
  // value this way avoids copying server state into state with an effect, which
  // is the usual way this screen goes wrong.
  const [draft, setDraft] = useState<string | null>(null)
  const [message, setMessage] = useState<ValidationMessage | undefined>(undefined)
  const [confirming, setConfirming] = useState(false)

  const nickname = draft ?? data?.displayName ?? ''

  async function handleRename(event: FormEvent) {
    event.preventDefault()

    const broken = validate({ displayName: nickname }, nicknameRules)
    setMessage(broken.displayName)

    if (broken.displayName !== undefined) {
      return
    }

    const failure = await rename.submit(async () => {
      await renameVehicle(vehicleId, nickname)
    })

    if (failure === null) {
      // Back to deriving from the server, then ask it again. The PATCH response
      // is discarded on purpose: one source for what is on screen beats two that
      // can disagree, and this costs a single request.
      setDraft(null)
      reload()
    }
  }

  async function handleDelete() {
    const failure = await removal.submit(async () => {
      await deleteVehicle(vehicleId)
    })

    if (failure === null) {
      void navigate(paths.garage)
    }
  }

  return (
    <>
      <h1>{data === null ? t('screens.vehicleDetails') : vehicleLabel(data)}</h1>

      <p><Link to={paths.garage}>{t('vehicle.backToGarage')}</Link></p>

      {loading && <p role="status">{t('common.loading')}</p>}

      {error !== null && <p role="alert">{t(errorMessageKey(error.code))}</p>}

      {data !== null && (
        <>
          <dl>
            <dt>{t('fields.registrationNumber')}</dt>
            <dd>{data.registrationNumber}</dd>
            <dt>{t('fields.make')}</dt>
            <dd>{data.make}</dd>
            <dt>{t('fields.commercialDescription')}</dt>
            <dd>{data.commercialDescription}</dd>
            <dt>{t('fields.vin')}</dt>
            <dd>{data.vin}</dd>
          </dl>

          <form onSubmit={(event) => { void handleRename(event) }} noValidate>
            <FormError error={rename.error} />

            <TextField
              label={t('fields.displayName')}
              value={nickname}
              onChange={(value) => { setDraft(value) }}
              message={message}
            />

            <button type="submit" disabled={rename.pending}>{t('vehicle.rename')}</button>
          </form>

          <FormError error={removal.error} />

          {confirming
            ? (
              <div>
                <p>{t('vehicle.confirmDelete')}</p>
                <button
                  type="button"
                  onClick={() => { void handleDelete() }}
                  disabled={removal.pending}
                >
                  {t('vehicle.confirmDeleteYes')}
                </button>
                <button type="button" onClick={() => { setConfirming(false) }}>
                  {t('vehicle.cancel')}
                </button>
              </div>
              )
            : (
              <button type="button" onClick={() => { setConfirming(true) }}>
                {t('vehicle.delete')}
              </button>
              )}
        </>
      )}
    </>
  )
}