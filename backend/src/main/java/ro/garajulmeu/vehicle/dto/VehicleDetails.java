package ro.garajulmeu.vehicle.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * One vehicle, with the identity its certificate carries. Phase 8 widens this to
 * the whole certificate.
 */
public record VehicleDetails(
		UUID id,
		String displayName,
		String registrationNumber,
		String make,
		String commercialDescription,
		String vin,
		Instant createdAt) {
}