package ro.garajulmeu.vehicle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Everything needed to put a vehicle in a garage.
 *
 * <p>Specification section 8 states the rule this record encodes: "Minimum
 * required fields to save a vehicle: A registration number, D.1 make, D.3
 * commercial description/model, E VIN." Those are also exactly the four columns
 * section 10.3 declares NOT NULL, so the requirement is stated twice and agrees
 * with itself. Phase 8 adds the thirty optional ones.
 *
 * <p>The VIN is not checked for its seventeen characters here. Section 13 places
 * semantic validation of the VIN inside the OCR review that arrives in Phase 9,
 * and refusing a short VIN typed by hand would lock out the vehicles that
 * legitimately have one - anything built before the standard, and some special
 * categories. The lengths are the column widths, so nothing can be truncated
 * silently on the way in.
 */
public record CreateVehicleRequest(

		@NotBlank @Size(max = 32) String registrationNumber,

		@NotBlank @Size(max = 64) String make,

		@NotBlank @Size(max = 128) String commercialDescription,

		@NotBlank @Size(max = 32) String vin,

		/** Optional from the start: a vehicle is identified by its certificate. */
		@Size(max = 120) String displayName) {
}