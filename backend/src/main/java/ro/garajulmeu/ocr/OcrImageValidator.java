package ro.garajulmeu.ocr;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;

/**
 * Decides whether an upload is an image we are willing to pay to have read.
 * Specification section 22: validate the MIME type, the file size and the
 * decoded image.
 *
 * <p><strong>The client's declared content type is ignored.</strong> A
 * declaration is an assertion, not evidence: anything can claim to be a JPEG.
 * The format is taken from the bytes, and the type carried onwards is the one
 * that was actually found - so a PNG uploaded as {@code image/jpeg} reaches the
 * provider correctly labelled, and an executable uploaded as {@code image/png}
 * does not reach it at all.
 *
 * <p>Dimensions are read from the header <strong>before</strong> the image is
 * decoded. A few kilobytes of PNG can legitimately declare a canvas of tens of
 * thousands of pixels a side, and decoding one to find out how big it is turns
 * the sender's file into our memory problem.
 *
 * <p>Nothing here logs the image or any part of it, per section 24.
 */
@Component
public class OcrImageValidator {

	private static final Logger log = LoggerFactory.getLogger(OcrImageValidator.class);

	private final OcrProperties properties;

	OcrImageValidator(OcrProperties properties) {
		this.properties = properties;
	}

	public OcrImage accept(byte[] bytes) {
		if (bytes.length == 0 || bytes.length > properties.maxUploadBytes()) {
			throw refuse("size", bytes.length);
		}

		try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
			if (stream == null) {
				throw refuse("unreadable", bytes.length);
			}

			Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
			if (!readers.hasNext()) {
				// Not an image at all, whatever the request called it.
				throw refuse("not an image", bytes.length);
			}

			ImageReader reader = readers.next();
			try {
				reader.setInput(stream);
				return inspect(reader, bytes);
			} finally {
				reader.dispose();
			}
		} catch (IOException exception) {
			throw refuse("unreadable", bytes.length);
		}
	}

	private OcrImage inspect(ImageReader reader, byte[] bytes) throws IOException {
		String contentType = contentTypeOf(reader.getFormatName());

		int width = reader.getWidth(0);
		int height = reader.getHeight(0);

		if ((long) width * height > properties.maxPixels()) {
			throw refuse("too many pixels", bytes.length);
		}
		if (width < properties.minSide() || height < properties.minSide()) {
			throw refuse("too small to read", bytes.length);
		}

		BufferedImage decoded = reader.read(0);
		if (decoded == null) {
			throw refuse("would not decode", bytes.length);
		}

		return new OcrImage(bytes, contentType, width, height);
	}

	/** JPEG and PNG only. A certificate is photographed, not typeset. */
	private String contentTypeOf(String formatName) {
		String format = formatName.toLowerCase(Locale.ROOT);

		if (format.equals("jpeg") || format.equals("jpg")) {
			return "image/jpeg";
		}
		if (format.equals("png")) {
			return "image/png";
		}
		throw refuse("unsupported format", 0);
	}

	/** The reason is logged, the bytes never are. */
	private ApiException refuse(String reason, int size) {
		log.info("Refused an OCR upload: {} ({} bytes)", reason, size);
		return new ApiException(ErrorCode.OCR_FILE_INVALID);
	}
}