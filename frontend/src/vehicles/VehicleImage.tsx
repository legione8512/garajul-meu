import { useRef, useState, type ChangeEvent } from 'react'
import { useTranslation } from 'react-i18next'

import { deleteVehicleImage, uploadVehicleImage } from '../api/endpoints/vehicles.ts'
import { FormError } from '../components/FormError.tsx'
import { useSubmission } from '../forms/useSubmission.ts'
import { errorMessageKey } from '../i18n/errorKey.ts'
import { useVehicleImage } from './useVehicleImage.ts'

interface Props {
  readonly vehicleId: string
  /** What the vehicle said when the screen loaded. Owned locally from then on. */
  readonly hasImage: boolean
}

/**
 * The photograph of one vehicle: shown, added, replaced, removed.
 *
 * <p><strong>Presence is held here rather than re-read from the vehicle.</strong>
 * Reloading the vehicle after an upload would empty the whole screen while the
 * request was in flight, which is the same reason the rename on the parent
 * screen keeps its answer instead of fetching again. The version counter beside
 * it is what forces a refetch after a replacement: the address is the vehicle's
 * and never changes, so nothing else would say the bytes behind it have.
 *
 * <p>The file input is cleared after every choice. Without that, choosing the
 * same file twice in a row fires no change event at all - the value has not
 * changed - and the second attempt would appear to do nothing.
 *
 * <p>No client-side size check, deliberately. The limit lives in the backend's
 * configuration and duplicating it here would be two numbers free to disagree;
 * IMAGE_TOO_LARGE already has a translation. The cost is that the browser
 * uploads the whole file before hearing that it was too big, which is bearable
 * at five megabytes and would not be at fifty.
 */
export function VehicleImage({ vehicleId, hasImage }: Props) {
  const { t } = useTranslation()

  const [present, setPresent] = useState(hasImage)
  const [version, setVersion] = useState(0)
  const [confirming, setConfirming] = useState(false)

  const upload = useSubmission()
  const removal = useSubmission()
  const inputRef = useRef<HTMLInputElement>(null)

  const { url, error, loading } = useVehicleImage(vehicleId, present, version)

  async function handleChosen(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    event.target.value = ''

    if (file === undefined) {
      return
    }

    const failure = await upload.submit(async () => {
      await uploadVehicleImage(vehicleId, file)
    })

    if (failure === null) {
      setPresent(true)
      setVersion(previous => previous + 1)
    }
  }

  async function handleDelete() {
    const failure = await removal.submit(async () => {
      await deleteVehicleImage(vehicleId)
    })

    if (failure === null) {
      setPresent(false)
      setConfirming(false)
    }
  }

  return (
    <section>
      <h2>{t('vehicleImage.title')}</h2>

      {loading && <p role="status">{t('common.loading')}</p>}

      {error !== null && <p role="alert">{t(errorMessageKey(error.code))}</p>}

      {url !== null && <img src={url} alt={t('vehicleImage.alt')} />}

      {!present && !loading && <p>{t('vehicleImage.none')}</p>}

      <FormError error={upload.error} />

      <p>
        <label>
          {present ? t('vehicleImage.replace') : t('vehicleImage.choose')}
          <input
            ref={inputRef}
            type="file"
            accept="image/jpeg,image/png"
            disabled={upload.pending}
            onChange={(event) => { void handleChosen(event) }}
          />
        </label>
      </p>

      <p>{t('vehicleImage.accepted')}</p>

      {upload.pending && <p role="status">{t('vehicleImage.uploading')}</p>}

      {present && (
        <>
          <FormError error={removal.error} />

          {confirming
            ? (
              <div>
                <p>{t('vehicleImage.confirmDelete')}</p>
                <button
                  type="button"
                  onClick={() => { void handleDelete() }}
                  disabled={removal.pending}
                >
                  {t('vehicleImage.confirmDeleteYes')}
                </button>
                <button type="button" onClick={() => { setConfirming(false) }}>
                  {t('vehicleImage.cancel')}
                </button>
              </div>
              )
            : (
              <button type="button" onClick={() => { setConfirming(true) }}>
                {t('vehicleImage.delete')}
              </button>
              )}
        </>
      )}
    </section>
  )
}