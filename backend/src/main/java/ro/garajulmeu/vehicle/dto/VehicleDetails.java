package ro.garajulmeu.vehicle.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * One vehicle, with the identity its certificate carries.
 *
 * <p><strong>{@code hasImage} rather than a URL, and that is a decision with a
 * date on it.</strong> Section 16 allows a vehicle response to carry either a
 * short-lived signed URL or an application endpoint. The endpoint is known from
 * the vehicle's own identifier, so sending it would be sending back something
 * the client already has - and it could not be used as an {@code <img src>}
 * anyway, because a browser sends no Authorization header on an image request.
 * The client fetches the bytes and makes an object URL. When the R2 provider
 * arrives it can produce a genuine signed URL, which needs no header and belongs
 * in a field of its own; this flag stays useful either way, since it is the one
 * thing the client cannot work out for itself.
 *
 * <p>The object key is deliberately absent. Nothing outside the backend has any
 * use for it, and a key in a response is a key in a log somewhere later.
 */
public record VehicleDetails(
		UUID id,
		String displayName,
		String registrationNumber,
		String make,
		String commercialDescription,
		String vin,
		Instant createdAt,
		boolean hasImage) {
}