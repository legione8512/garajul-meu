package ro.garajulmeu.storage;

/**
 * An upload that has been checked and may be stored.
 *
 * <p>Existing only as the result of {@link VehicleImageValidator} is the point:
 * nothing else can construct one, so no path reaches the bucket without having
 * passed the checks. The same arrangement as {@code OcrImage}.
 *
 * <p>No width or height. Section 10.3 stores the key, the content type and the
 * size, and dimensions were measured only to refuse the file - keeping a number
 * nothing reads would be a column waiting to go stale.
 *
 * <p>The array is not defensively copied and equality is therefore by reference.
 * Neither matters: the value lives for one request and is never compared.
 */
public record VehicleImage(byte[] bytes, String contentType) {

	public long sizeBytes() {
		return bytes.length;
	}
}