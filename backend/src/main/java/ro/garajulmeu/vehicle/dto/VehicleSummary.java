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
 */
public record VehicleSummary(
		UUID id,
		String displayName,
		String registrationNumber,
		String make,
		String commercialDescription) {
}