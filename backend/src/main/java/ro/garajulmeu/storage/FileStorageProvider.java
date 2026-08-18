package ro.garajulmeu.storage;

import java.util.Optional;

/**
 * Object storage, behind an interface so business logic never touches an S3 SDK.
 * Specification sections 22 and 32, which name the pair directly:
 * {@code FileStorageProvider → CloudflareR2FileStorageProvider}.
 *
 * <p><strong>Keys in, bytes out, and deliberately no URLs.</strong> Section 16
 * allows a vehicle's image to be reached either through a short-lived signed URL
 * or through an application endpoint, and this supports the second. A signed URL
 * is a Cloudflare concept that a filesystem cannot produce, so putting one in
 * this interface would make the development provider a lie - and a seam whose
 * two implementations mean different things by the same method is worse than no
 * seam. The cost is that every read passes through the backend instead of going
 * straight to the object; for a garage of a few vehicles that is a fair price
 * for a bucket that stays private with no expiry machinery. A {@code urlFor}
 * can be added the day the traffic argues for it, and not before.
 *
 * <p>The content type is given on write, because R2 stores it on the object and
 * a later signed GET would serve it from there. It is <strong>not</strong>
 * returned on read: section 22 puts object metadata in PostgreSQL, so
 * {@code vehicles.image_content_type} is the authority and a second copy could
 * only ever disagree with it.
 *
 * <p>Object keys are UUID-derived, per section 22 - no email, registration
 * number, owner name or VIN anywhere in the path. That is what makes a key safe
 * to write into a log line, which the implementations do.
 */
public interface FileStorageProvider {

	/**
	 * Writes, replacing whatever was at that key.
	 *
	 * @throws ro.garajulmeu.exception.ApiException with
	 *         STORAGE_PROVIDER_UNAVAILABLE when the store cannot be reached. The
	 *         caller decides what a failed write means; nothing here does.
	 */
	void put(String objectKey, byte[] bytes, String contentType);

	/**
	 * Empty when nothing is at that key, which is a fact rather than a failure -
	 * an object removed outside the application leaves a row pointing at nothing,
	 * and the screen that reads it should show a vehicle without a photograph
	 * rather than an error.
	 */
	Optional<byte[]> get(String objectKey);

	/** Idempotent: deleting a key that is not there succeeds and says nothing. */
	void delete(String objectKey);
}