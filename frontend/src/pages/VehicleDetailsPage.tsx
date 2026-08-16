import { useRef, useState, type FormEvent } from 'react'
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

  const { data, error, loading } = useResource<VehicleDetails>(vehiclePath(vehicleId))

  const rename = useSubmission()
  const removal = useSubmission()

  // Null means "showing whatever the server last said". Deriving the input's
  // value this way avoids copying server state into state with an effect, which
  // is the usual way this screen goes wrong.
  const [draft, setDraft] = useState<string | null>(null)
  const [message, setMessage] = useState<ValidationMessage | undefined>(undefined)
  const [confirming, setConfirming] = useState(false)

  /**
   * The renamed vehicle, once the server has confirmed it.
   *
   * <p>Held here rather than fetched again. Reloading would empty the resource
   * while the request was in flight - useResource reports data only for the
   * request currently on screen - and the whole page, list, form and delete
   * button, would vanish and come back on every rename. The PATCH already
   * answers with the vehicle; asking twice is both slower and a window in which
   * the screen shows nothing.
   */
  const [renamed, setRenamed] = useState<VehicleDetails | null>(null)
  const fresh = useRef<VehicleDetails | null>(null)

  const vehicle = renamed ?? data
  const nickname = draft ?? vehicle?.displayName ?? ''

  async function handleRename(event: FormEvent) {
    event.preventDefault()

    const broken = validate({ displayName: nickname }, nicknameRules)
    setMessage(broken.displayName)

    if (broken.displayName !== undefined) {
      return
    }

    const failure = await rename.submit(async () => {
      fresh.current = await renameVehicle(vehicleId, nickname)
    })

    if (failure === null && fresh.current !== null) {
      setRenamed(fresh.current)
      setDraft(null)
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
      <h1>{vehicle === null ? t('screens.vehicleDetails') : vehicleLabel(vehicle)}</h1>

      <p><Link to={paths.garage}>{t('vehicle.backToGarage')}</Link></p>
      <p><Link to={paths.certificate(vehicleId)}>{t('certificate.open')}</Link></p>

      {loading && <p role="status">{t('common.loading')}</p>}

      {error !== null && <p role="alert">{t(errorMessageKey(error.code))}</p>}

      {vehicle !== null && (
        <>
          <dl>
            <dt>{t('fields.registrationNumber')}</dt>
            <dd>{vehicle.registrationNumber}</dd>
            <dt>{t('fields.make')}</dt>
            <dd>{vehicle.make}</dd>
            <dt>{t('fields.commercialDescription')}</dt>
            <dd>{vehicle.commercialDescription}</dd>
            <dt>{t('fields.vin')}</dt>
            <dd>{vehicle.vin}</dd>
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