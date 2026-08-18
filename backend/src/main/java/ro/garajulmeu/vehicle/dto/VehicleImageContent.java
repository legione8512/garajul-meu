package ro.garajulmeu.vehicle.dto;

/**
 * A stored photograph on its way back out.
 *
 * <p>Separate from {@code storage.VehicleImage} on purpose: that type exists
 * only as the result of validation, so that nothing can reach the bucket without
 * having been checked. Bytes coming *out* were checked when they went in, and
 * reusing the type would quietly weaken the claim its documentation makes.
 *
 * <p>The content type comes from the database rather than from the object,
 * because section 22 puts object metadata in PostgreSQL.
 */
public record VehicleImageContent(byte[] bytes, String contentType) {
}