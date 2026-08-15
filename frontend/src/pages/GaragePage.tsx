import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'

import { useResource } from '../api/useResource.ts'
import { vehicleLabel, vehiclesPath, type VehicleSummary } from '../api/endpoints/vehicles.ts'
import { errorMessageKey } from '../i18n/errorKey.ts'
import { paths } from '../routes/paths.ts'

/**
 * Screen 7 in specification section 5.
 *
 * <p>Loading, failed and answered are mutually exclusive by construction: the
 * hook reports data and error only for the request currently on screen, so the
 * three branches below cannot overlap and none needs to guard against another.
 */
export function GaragePage() {
  const { t } = useTranslation()
  const { data, error, loading, reload } = useResource<VehicleSummary[]>(vehiclesPath)

  return (
    <>
      <h1>{t('screens.garage')}</h1>

      <p><Link to={paths.addVehicle}>{t('garage.add')}</Link></p>

      {loading && <p role="status">{t('common.loading')}</p>}

      {error !== null && (
        <div role="alert">
          <p>{t(errorMessageKey(error.code))}</p>
          <button type="button" onClick={reload}>{t('common.retry')}</button>
        </div>
      )}

      {data !== null && data.length === 0 && <p>{t('garage.empty')}</p>}

      {data !== null && data.length > 0 && (
        <ul>
          {data.map(vehicle => (
            <li key={vehicle.id}>
              <span>{vehicleLabel(vehicle)}</span>
              <span>{vehicle.registrationNumber}</span>
            </li>
          ))}
        </ul>
      )}
    </>
  )
}