package ro.garajulmeu.vehicle.dto;

import java.util.UUID;

/**
 * One line in the garage.
 *
 * <p>{@code displayName} is whatever the owner chose and is often absent;
 * labelling the vehicle from the certificate when it is missing is the client's
 * decision, not the API's, so both are sent as stored.
 *
 * <p>The VIN is deliberately not here. A list does not need it, and section 24
 * argues for carrying personal or identifying data only where it is used.
 *
 * <p>{@code hasImage} was added in 1.0.2 for the thumbnail on the card. Whether
 * there is a photograph, never where it is: the address is derivable from the
 * vehicle's own identifier, and the object key is never sent to anybody. It is
 * the one thing about the picture a list cannot work out for itself, and without
 * it every card would ask for a photograph that mostly is not there.
 */
public record VehicleSummary(
		UUID id,
		String displayName,
		String registrationNumber,
		String make,
		String commercialDescription,
		boolean hasImage) {
}