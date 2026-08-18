package ro.garajulmeu.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 * Whether a pile of bytes is an image we are willing to accept, and what kind.
 * Specification section 22: validate the MIME type, the file size and the
 * decoded image.
 *
 * <p><strong>This answers facts and never decides what they mean.</strong> It
 * throws nothing and knows no error codes, because two callers want different
 * answers to the same finding: OCR flattens every refusal into
 * OCR_FILE_INVALID, since a caller who sent the wrong thing gets the same
 * advice either way, while a vehicle image separates IMAGE_TOO_LARGE from
 * IMAGE_INVALID_TYPE - one is worth retrying with a smaller file and the other
 * is not. Putting the codes in here would have forced one of those to be wrong.
 *
 * <p><strong>The client's declared content type is ignored.</strong> A
 * declaration is an assertion, not evidence: anything can claim to be a JPEG.
 * The format is taken from the bytes, and the type reported is the one that was
 * actually found - so a PNG uploaded as {@code image/jpeg} is stored correctly
 * labelled, and an executable uploaded as {@code image/png} is not stored at all.
 *
 * <p>Dimensions are read from the header <strong>before</strong> the image is
 * decoded. A few kilobytes of PNG can legitimately declare a canvas of tens of
 * thousands of pixels a side, and decoding one to find out how big it is turns
 * the sender's file into our memory problem.
 *
 * <p>Nothing here logs the image or any part of it, per section 24. It does not
 * log at all: the caller knows which upload this was and what it is for.
 */
public final class ImageInspection {

	private ImageInspection() {
	}

	/**
	 * Why an upload was not accepted. Seven reasons rather than one boolean,
	 * because the reason is the only thing the caller cannot work out for itself.
	 */
	public enum Refusal {

		/** Nothing was sent. */
		EMPTY,

		/** More bytes than the caller allows. */
		TOO_LARGE,

		/** No reader recognised it. Whatever it is, it is not a picture. */
		NOT_AN_IMAGE,

		/** A real image in a format we do not keep. */
		UNSUPPORTED_FORMAT,

		/** Width times height above the ceiling - refused from the header, undecoded. */
		TOO_MANY_PIXELS,

		/** Smaller than the caller's smallest useful edge. */
		TOO_SMALL,

		/** Recognised, then failed part-way through. A truncated upload looks like this. */
		UNREADABLE
	}

	/**
	 * What one caller will accept. A record rather than three arguments so that
	 * adding a fourth limit does not silently reorder anybody's call.
	 */
	public record Limits(long maxBytes, long maxPixels, int minSide) {
	}

	public sealed interface Result permits Accepted, Refused {
	}

	/**
	 * @param contentType determined from the bytes, never from the request
	 */
	public record Accepted(String contentType, int width, int height) implements Result {
	}

	public record Refused(Refusal reason) implements Result {
	}

	public static Result inspect(byte[] bytes, Limits limits) {
		if (bytes.length == 0) {
			return new Refused(Refusal.EMPTY);
		}
		if (bytes.length > limits.maxBytes()) {
			return new Refused(Refusal.TOO_LARGE);
		}

		try (ImageInputStream stream =
				ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {

			if (stream == null) {
				return new Refused(Refusal.UNREADABLE);
			}

			Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
			if (!readers.hasNext()) {
				return new Refused(Refusal.NOT_AN_IMAGE);
			}

			ImageReader reader = readers.next();
			try {
				reader.setInput(stream);
				return measure(reader, limits);
			} finally {
				reader.dispose();
			}
		} catch (IOException exception) {
			return new Refused(Refusal.UNREADABLE);
		}
	}

	private static Result measure(ImageReader reader, Limits limits) throws IOException {
		String contentType = contentTypeOf(reader.getFormatName());

		if (contentType == null) {
			return new Refused(Refusal.UNSUPPORTED_FORMAT);
		}

		int width = reader.getWidth(0);
		int height = reader.getHeight(0);

		// Both from the header. Order matters: an enormous canvas is refused
		// before anything asks the decoder to produce it.
		if ((long) width * height > limits.maxPixels()) {
			return new Refused(Refusal.TOO_MANY_PIXELS);
		}
		if (width < limits.minSide() || height < limits.minSide()) {
			return new Refused(Refusal.TOO_SMALL);
		}
		if (reader.read(0) == null) {
			return new Refused(Refusal.UNREADABLE);
		}

		return new Accepted(contentType, width, height);
	}

	/**
	 * JPEG and PNG only, and null for everything else.
	 *
	 * <p>A certificate is photographed and so is a car; neither is typeset. GIF
	 * and BMP are refused as a policy rather than as an accident - ImageIO reads
	 * both perfectly well, which is exactly why the list is stated rather than
	 * inherited from whatever the runtime happens to support.
	 */
	private static String contentTypeOf(String formatName) {
		String format = formatName.toLowerCase(Locale.ROOT);

		if (format.equals("jpeg") || format.equals("jpg")) {
			return "image/jpeg";
		}
		if (format.equals("png")) {
			return "image/png";
		}
		return null;
	}
}