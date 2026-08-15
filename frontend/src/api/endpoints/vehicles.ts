/**
 * The vehicle surface of the API.
 *
 * <p>This module exports a path rather than a fetching function, unlike its
 * neighbours. That is deliberate: useResource keys its effect on the path
 * string, and a string is stable between renders where a function is not. A
 * fetcher would have to be memoised at every call site or the screen would
 * reload itself forever.
 */
export const vehiclesPath = '/api/v1/vehicles'

export interface VehicleSummary {
  readonly id: string
  /**
   * Optional twice over: the field itself is optional, and the backend omits
   * null values from JSON rather than sending them. Typing it as both is the
   * only honest description of what can actually arrive.
   */
  readonly displayName?: string | null
  readonly registrationNumber: string
  readonly make: string
  readonly commercialDescription: string
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