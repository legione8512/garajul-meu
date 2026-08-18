package ro.garajulmeu.ocr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ro.garajulmeu.common.ImageInspection;
import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;

/**
 * Decides whether an upload is an image we are willing to pay to have read.
 *
 * <p>The checking itself lives in {@link ImageInspection}, which phase 12 shares
 * - the rules are about bytes and are the same wherever an image arrives. What
 * stays here is the part that is about OCR: the limits come from
 * {@link OcrProperties}, and <strong>every refusal answers the same code</strong>.
 *
 * <p>That flattening is deliberate rather than lazy. A caller who sent a PDF, an
 * empty part, a thumbnail or a decompression bomb gets the same useful advice -
 * send a photograph of the certificate - and telling them which of the four it
 * was would describe our checks rather than their problem. Phase 12 makes the
 * opposite choice for the same findings, because there IMAGE_TOO_LARGE is worth
 * retrying with a smaller file and IMAGE_INVALID_TYPE is not.
 *
 * <p>The reason is logged, the bytes never are, per section 24.
 */
@Component
public class OcrImageValidator {

	private static final Logger log = LoggerFactory.getLogger(OcrImageValidator.class);

	private final OcrProperties properties;

	OcrImageValidator(OcrProperties properties) {
		this.properties = properties;
	}

	public OcrImage accept(byte[] bytes) {
		ImageInspection.Limits limits = new ImageInspection.Limits(
				properties.maxUploadBytes(), properties.maxPixels(), properties.minSide());

		return switch (ImageInspection.inspect(bytes, limits)) {
			case ImageInspection.Refused refused -> throw refuse(refused.reason(), bytes.length);
			case ImageInspection.Accepted accepted -> new OcrImage(
					bytes, accepted.contentType(), accepted.width(), accepted.height());
		};
	}

	private ApiException refuse(ImageInspection.Refusal reason, int size) {
		log.info("Refused an OCR upload: {} ({} bytes)", reason, size);
		return new ApiException(ErrorCode.OCR_FILE_INVALID);
	}
}