import { useEffect, useState } from 'react'

import { ApiError } from '../api/ApiError.ts'
import { fetchVehicleImage, fetchVehicleThumbnail } from '../api/endpoints/vehicles.ts'

/** Which of the two stored pictures: the photograph, or the card's small square. */
type Size = 'full' | 'thumbnail'

export interface VehicleImageState {
  /** An object URL, or null while there is nothing to show. */
  readonly url: string | null
  readonly error: ApiError | null
  readonly loading: boolean
}

/**
 * Fetches one vehicle's photograph and hands back an address an `<img>` can use.
 *
 * <p><strong>This whole hook exists because a browser sends no Authorization
 * header on an image request.</strong> Pointing an `<img src>` at the protected
 * endpoint produces an unauthenticated request and a 401, so the bytes are
 * fetched like any other call and turned into an object URL.
 *
 * <p><strong>The object URL is revoked.</strong> Every createObjectURL keeps its
 * blob alive until the matching revoke, so without the cleanup below each
 * replacement would strand a few megabytes for the life of the page. The revoke
 * happens on unmount and on every re-run, which is why the URL is created inside
 * the effect rather than in the promise's caller.
 *
 * <p>Loading is derived rather than stored, for the same reason as useResource:
 * setting a flag at the top of an effect re-renders before the request has been
 * sent, which is what react-hooks/set-state-in-effect objects to. The one piece
 * of state records which request it answers.
 *
 * @param present whether the vehicle has a photograph at all. False fetches
 *                nothing - a 404 is the expected answer then, and asking for it
 *                would put a predictable error in the console on every screen
 *                for a vehicle nobody has photographed
 * @param version bumped by the caller after a replacement. The address never
 *                changes, so nothing else would tell this hook to look again
 */
export function useVehicleImage(
  vehicleId: string,
  present: boolean,
  version: number,
): VehicleImageState {
  return useStoredImage(vehicleId, present, version, 'full')
}

/**
 * The same thing for the card's thumbnail (1.0.2), which has no version because
 * nothing replaces a picture from a card: the screen that does is the vehicle's,
 * and coming back to a card mounts this afresh.
 */
export function useVehicleThumbnail(vehicleId: string, present: boolean): VehicleImageState {
  return useStoredImage(vehicleId, present, 0, 'thumbnail')
}

/**
 * The size travels as a value rather than as a fetcher function, deliberately: a
 * function argument would be a new identity on every render and the effect below
 * keys on its dependencies, so the screen would fetch itself in a loop. A string
 * is stable, which is the same reason the API exposes reads as paths.
 */
function useStoredImage(
  vehicleId: string,
  present: boolean,
  version: number,
  size: Size,
): VehicleImageState {
  const [settled, setSettled] = useState<{
    key: string
    url: string | null
    error: ApiError | null
  }>({ key: '', url: null, error: null })

  const key = `${vehicleId}#${size}#${String(version)}`

  useEffect(() => {
    if (!present) {
      return
    }

    // Guards against a response arriving after the screen has been left, and
    // carries the created URL out to the cleanup so it can be revoked.
    let current = true
    let created: string | null = null

    const asked = size === 'full'
      ? fetchVehicleImage(vehicleId)
      : fetchVehicleThumbnail(vehicleId)

    asked
      .then(blob => {
        if (!current) {
          return
        }
        created = URL.createObjectURL(blob)
        setSettled({ key, url: created, error: null })
      })
      .catch((cause: unknown) => {
        if (current) {
          setSettled({
            key,
            url: null,
            error: cause instanceof ApiError ? cause : new ApiError('UNKNOWN', 0, null, []),
          })
        }
      })

    return () => {
      current = false
      if (created !== null) {
        URL.revokeObjectURL(created)
      }
    }
  }, [key, present, size, vehicleId])

  const answered = settled.key === key

  return {
    url: present && answered ? settled.url : null,
    error: present && answered ? settled.error : null,
    loading: present && !answered,
  }
}