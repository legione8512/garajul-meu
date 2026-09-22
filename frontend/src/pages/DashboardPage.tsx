import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'

import { dashboardPath, type DashboardView } from '../api/endpoints/dashboard.ts'
import { vehicleLabel } from '../api/endpoints/vehicles.ts'
import { useResource } from '../api/useResource.ts'
import { dateFormatter, stateOf } from '../documents/status.ts'
import { errorMessageKey } from '../i18n/errorKey.ts'
import { paths } from '../routes/paths.ts'
import { BrandMark } from '../vehicles/BrandMark.tsx'
import { VehicleThumbnail } from '../vehicles/VehicleThumbnail.tsx'

/**
 * Screen 6 in specification section 5, with the content section 27 asks of it:
 * "web users always see current/urgent expiry state on the Dashboard".
 *
 * <p>All four types are shown for every vehicle, including the ones nothing has
 * been entered for. For a new account that is four "not configured" lines per
 * car, which is the point rather than noise - it is the only place the
 * application says what it is for, and each vehicle carries a link to the screen
 * that answers it. An invitation, not an alarm.
 *
 * <p>The whole card opens the vehicle, since 2026-09-14 on an iPad, where a
 * card the size of a hand answered only to the name in its corner. It is still
 * one link - the name - stretched over the card by `data-card-link` in
 * index.css, so a screen reader hears one link per vehicle, named by the
 * vehicle. The documents link sits above the stretched area and goes where it
 * says.
 *
 * <p>The make's emblem sits on the right of the card when the make is one
 * `vehicles/brandMarks.ts` knows, and nothing sits there when it is not
 * (2026-09-14, at the developer's request).
 *
 * <p>The owner's photograph sits on the left of the name, round and small, since
 * 1.0.2 - beside the name rather than above the lines, because a picture on its
 * own row would push four document lines off a telephone screen and indenting
 * the lines behind it would wrap every one of them.
 */
export function DashboardPage() {
  const { t, i18n } = useTranslation()
  const { data, error, loading } = useResource<DashboardView>(dashboardPath)

  const formatDate = dateFormatter(i18n.language)

  return (
    <>
      <h1>{t('screens.dashboard')}</h1>

      {loading && <p role="status">{t('common.loading')}</p>}

      {error !== null && <p role="alert">{t(errorMessageKey(error.code))}</p>}

      {data !== null && data.vehicles.length === 0 && (
        <>
          <p>{t('dashboard.empty')}</p>
          <p><Link data-action="primary" to={paths.addVehicle}>{t('garage.add')}</Link></p>
        </>
      )}

      {data !== null && data.vehicles.map(vehicle => (
        <section data-card data-card-link key={vehicle.vehicleId}>
          <div data-card-head>
            <VehicleThumbnail vehicleId={vehicle.vehicleId} hasImage={vehicle.hasImage} />
            <h2>
              <Link to={paths.vehicle(vehicle.vehicleId)}>{vehicleLabel(vehicle)}</Link>
            </h2>
          </div>

          <ul>
            {vehicle.documents.map((line) => {
              const state = stateOf(line, formatDate)

              return (
                <li key={line.type} data-tone={state.tone}>
                  <span>{t(`documents.type.${line.type}`)}</span>
                  {' — '}
                  <span>{t(state.key, state.values)}</span>
                </li>
              )
            })}
          </ul>

          <p>
            <Link to={paths.documents(vehicle.vehicleId)}>{t('dashboard.configure')}</Link>
          </p>

          <BrandMark make={vehicle.make} />
        </section>
      ))}
    </>
  )
}