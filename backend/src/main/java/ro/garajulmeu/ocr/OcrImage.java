package ro.garajulmeu.ocr;

/**
 * An upload that has been checked and may be sent to a provider.
 *
 * <p>The content type is the one <strong>determined from the bytes</strong>, not
 * the one the client declared. Existing only as the result of
 * {@link OcrImageValidator} is the point: nothing else can construct a value of
 * this type, so no path reaches a provider without having passed the checks.
 *
 * <p>The array is not defensively copied and the record's equality is therefore
 * by reference. Neither matters here - the value lives for one request and is
 * never compared - and copying ten megabytes to feel tidy would not.
 */
public record OcrImage(byte[] bytes, String contentType, int width, int height) {
}