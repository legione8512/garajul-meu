import { VEHICLE_IMAGE_CEILING, withinCeiling } from '../../images/shrink.ts'
import { apiFetch, apiFetchBlob } from '../client.ts'
/**
 * The vehicle surface of the API.
 *
 * <p>Reads are exposed as paths and writes as functions, which looks
 * inconsistent and is not. useResource keys its effect on the path string, and a
 * string is stable between renders where a function is not - a fetcher would
 * have to be memoised at every call site or the screen would reload itself
 * forever. A write is called once, from an event handler, and has no such
 * problem.
 */
export const vehiclesPath = '/api/v1/vehicles'

export function vehiclePath(vehicleId: string): string {
  return `${vehiclesPath}/${vehicleId}`
}

export function vehicleImagePath(vehicleId: string): string {
  return `${vehiclePath(vehicleId)}/image`
}

export interface VehicleSummary {
  readonly id: string
  /**
   * Optional in the API and null on the wire: the field itself is optional, and
   * the backend sends an explicit JSON null when there is no nickname rather
   * than omitting the key - there is no NON_NULL configuration anywhere on that
   * side. Declaring both is what makes `??` correct whichever one arrives, and
   * costs nothing if the backend ever starts omitting them.
   */
  readonly displayName?: string | null
  readonly registrationNumber: string
  readonly make: string
  readonly commercialDescription: string
}

export interface VehicleDetails extends VehicleSummary {
  readonly vin: string
  readonly createdAt: string
  /**
   * Whether a photograph exists - not where it is.
   *
   * <p>The address is derivable from the vehicle's own identifier, so sending it
   * would return something the client already has, and it could not be used as
   * an `<img src>` regardless. The object key is deliberately never sent at all.
   * When the R2 provider arrives it can produce a genuine signed URL, which
   * needs no header and would belong in a field of its own; this flag stays
   * useful either way, being the one thing the client cannot work out itself.
   */
  readonly hasImage: boolean
}

/**
 * The four fields section 8 names as the minimum to save a vehicle, plus the
 * optional nickname. The rest of the certificate arrives in Phase 8.
 */
export interface NewVehicle {
  readonly registrationNumber: string
  readonly make: string
  readonly commercialDescription: string
  readonly vin: string
  readonly displayName?: string
}

export function createVehicle(vehicle: NewVehicle): Promise<VehicleDetails> {
  return apiFetch<VehicleDetails>(vehiclesPath, {
    method: 'POST',
    body: JSON.stringify(vehicle),
  })
}

/**
 * A blank nickname removes it. The convention across this API is that an absent
 * field is left alone, which leaves an optional value no way to be cleared - so
 * the backend reads an empty string as "remove", and this sends exactly what the
 * person left in the input.
 */
export function renameVehicle(vehicleId: string, displayName: string): Promise<VehicleDetails> {
  return apiFetch<VehicleDetails>(vehiclePath(vehicleId), {
    method: 'PATCH',
    body: JSON.stringify({ displayName }),
  })
}

export function deleteVehicle(vehicleId: string): Promise<void> {
  return apiFetch<void>(vehiclePath(vehicleId), { method: 'DELETE' })
}

/** The bytes, fetched with the token. The caller makes the object URL. */
export function fetchVehicleImage(vehicleId: string): Promise<Blob> {
  return apiFetchBlob(vehicleImagePath(vehicleId))
}

/**
 * The file itself as the request body, not multipart.
 *
 * <p>That is the backend's constraint rather than a preference: Tomcat parses
 * multipart for POST only, so on a PUT the parts would simply not be there. A
 * File is a Blob and fetch sends it as-is.
 *
 * <p>The Content-Type is set explicitly so the client does not label a
 * photograph as JSON. The backend ignores what we declare and reads the format
 * from the bytes - a declaration is an assertion, not evidence - so this header
 * is honesty rather than instruction.
 *
 * <p><strong>Shrunk first when it would not fit.</strong> Five megabytes is
 * reachable by an ordinary photograph taken on an ordinary phone, and until
 * 2026-08-29 nothing on this side did anything about it: the browser sent
 * whatever was chosen and the person met IMAGE_TOO_LARGE. It does not bite
 * often only because people pick existing pictures, which are usually already
 * compressed.
 */
export async function uploadVehicleImage(vehicleId: string, file: File): Promise<void> {
  const upload = await withinCeiling(file, VEHICLE_IMAGE_CEILING)

  return await apiFetch<void>(vehicleImagePath(vehicleId), {
    method: 'PUT',
    body: upload,
    headers: { 'Content-Type': upload.type === '' ? 'application/octet-stream' : upload.type },
  })
}

export function deleteVehicleImage(vehicleId: string): Promise<void> {
  return apiFetch<void>(vehicleImagePath(vehicleId), { method: 'DELETE' })
}

/**
 * What a vehicle is called on screen.
 *
 * <p>The backend sends the nickname and the certificate fields and lets the
 * client decide, which is the section 9 rule seen from this side: identity
 * belongs to the certificate, and the label is presentation. Make and
 * description together, because "Logan" alone does not distinguish two of them
 * in the same garage.
 */
export function vehicleLabel(
  vehicle: Pick<VehicleSummary, 'displayName' | 'make' | 'commercialDescription'>,
): string {
  return vehicle.displayName ?? `${vehicle.make} ${vehicle.commercialDescription}`
}