package ro.garajulmeu.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ro.garajulmeu.common.ImageInspection;
import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;

/**
 * Decides whether an upload is a photograph we are willing to keep.
 *
 * <p>The checking is {@link ImageInspection}'s, shared with OCR since 12.1 - the
 * rules are about bytes and do not change with the reason for having them. What
 * is different here is the answer.
 *
 * <p><strong>Two codes rather than OCR's one.</strong> A file refused for its
 * size is worth sending again smaller, and a file refused for what it is never
 * will be; telling the two apart is the difference between advice and a shrug.
 * OCR flattens the same findings deliberately, because there the advice is
 * identical either way.
 *
 * <p>The reason is logged, the bytes never are, per section 24.
 */
@Component
public class VehicleImageValidator {

	private static final Logger log = LoggerFactory.getLogger(VehicleImageValidator.class);

	private final StorageProperties properties;

	VehicleImageValidator(StorageProperties properties) {
		this.properties = properties;
	}

	public VehicleImage accept(byte[] bytes) {
		ImageInspection.Limits limits = new ImageInspection.Limits(
				properties.maxUploadBytes(), properties.maxPixels(), properties.minSide());

		return switch (ImageInspection.inspect(bytes, limits)) {
			case ImageInspection.Refused refused -> throw refuse(refused.reason(), bytes.length);
			case ImageInspection.Accepted accepted ->
					new VehicleImage(bytes, accepted.contentType());
		};
	}

	/**
	 * Every refusal reason, mapped to one of the two codes section 17 provides.
	 *
	 * <p>Exhaustive and without a default, which is the whole reason it is a
	 * method of its own: an eighth reason added to {@code ImageInspection} stops
	 * this compiling instead of quietly becoming whatever the default said.
	 *
	 * <p>TOO_MANY_PIXELS answers IMAGE_TOO_LARGE rather than IMAGE_INVALID_TYPE,
	 * because a decompression bomb is a size problem wearing a header. TOO_SMALL
	 * answers IMAGE_INVALID_TYPE, which reads oddly and is still the better of
	 * the two: section 17 fixes the catalogue, and inventing an IMAGE_TOO_SMALL
	 * would be a deviation for one wording.
	 */
	static ErrorCode codeFor(ImageInspection.Refusal reason) {
		return switch (reason) {
			case TOO_LARGE, TOO_MANY_PIXELS -> ErrorCode.IMAGE_TOO_LARGE;
			case EMPTY, NOT_AN_IMAGE, UNSUPPORTED_FORMAT, TOO_SMALL, UNREADABLE ->
					ErrorCode.IMAGE_INVALID_TYPE;
		};
	}

	private ApiException refuse(ImageInspection.Refusal reason, int size) {
		log.info("Refused a vehicle image: {} ({} bytes)", reason, size);
		return new ApiException(codeFor(reason));
	}
}