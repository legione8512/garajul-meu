import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'

import { vehicleLabel, vehiclesPath, type VehicleSummary } from '../api/endpoints/vehicles.ts'
import { useResource } from '../api/useResource.ts'
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

      {/*
        The explanation comes before the offer when there is nothing here. With
        vehicles in the garage the message does not render at all, so the button
        keeps its usual place above the list - one move rather than a branch.
      */}
      {data !== null && data.length === 0 && <p>{t('garage.empty')}</p>}

      <p><Link data-action="primary" to={paths.addVehicle}>{t('garage.add')}</Link></p>

      {loading && <p role="status">{t('common.loading')}</p>}

      {error !== null && (
        <div role="alert">
          <p>{t(errorMessageKey(error.code))}</p>
          <button type="button" onClick={reload}>{t('common.retry')}</button>
        </div>
      )}

      {/*
        The name and the plate were adjacent JSX elements, which produce no
        whitespace between them - the garage read "Chevrolet AveoCT 10 DOC" from
        the day the screen was written. A heading and a subtitle say what each of
        them is, and cannot run together.
      */}
      {data !== null && data.length > 0 && (
        <ul>
          {data.map(vehicle => (
            <li data-card key={vehicle.id}>
              <h2>
                <Link to={paths.vehicle(vehicle.id)}>{vehicleLabel(vehicle)}</Link>
              </h2>
              <p data-subtitle>{vehicle.registrationNumber}</p>
            </li>
          ))}
        </ul>
      )}
    </>
  )
}