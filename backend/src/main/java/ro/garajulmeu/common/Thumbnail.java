package ro.garajulmeu.common;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

/**
 * A small square picture of a car, made from the photograph its owner uploaded,
 * for the dashboard and garage cards added in 1.0.2.
 *
 * <p><strong>Why the server makes it.</strong> The alternative is sending the
 * original and letting CSS shrink it: on a garage of five cars that is up to
 * twenty-five megabytes over a phone connection to fill five circles the size of
 * a thumbnail. This costs one decode, once, and about eight kilobytes a card
 * afterwards.
 *
 * <p><strong>The original is never decoded at full size.</strong> A current
 * phone photographs at forty megapixels, which is a hundred and sixty megabytes
 * of {@code int[]} that this container does not have to spare. The reader is
 * asked for every n-th pixel instead, choosing n so that what comes back is
 * still at least twice the thumbnail - enough for the scaling step to average
 * over, and small enough that several at once are unremarkable.
 *
 * <p><strong>It answers facts and throws nothing</strong>, like
 * {@link ImageInspection} beside it. A photograph a reader cannot handle - a
 * CMYK JPEG is the usual one - gives an empty answer, and the two callers differ
 * on what that means: an upload carries on without a thumbnail, and a request
 * for one that cannot be made is a 404. Neither wants an exception, and a
 * failure here must never cost somebody their upload.
 */
public final class Thumbnail {

	/**
	 * The side, in pixels. The cards draw a 48-pixel circle, and a phone's screen
	 * is three device pixels to one of those - so 192 is what "sharp on the
	 * telephone it was made for" works out to, and there is no point storing more.
	 */
	public static final int SIDE = 192;

	/** Stored as JPEG whatever arrived: a car photograph is not a diagram. */
	public static final String CONTENT_TYPE = "image/jpeg";

	private static final float QUALITY = 0.85f;

	private Thumbnail() {
	}

	/** Empty when these bytes are not an image, or not one a reader can decode. */
	public static Optional<byte[]> of(byte[] original) {
		try (ImageInputStream stream =
				ImageIO.createImageInputStream(new ByteArrayInputStream(original))) {

			if (stream == null) {
				return Optional.empty();
			}

			Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
			if (!readers.hasNext()) {
				return Optional.empty();
			}

			ImageReader reader = readers.next();
			try {
				reader.setInput(stream);
				BufferedImage sampled = sample(reader);
				return Optional.of(encode(square(upright(sampled, JpegOrientation.of(original)))));
			} finally {
				reader.dispose();
			}
		} catch (IOException exception) {
			return Optional.empty();
		}
	}

	/**
	 * Decodes every n-th pixel, with n chosen from the shorter side because that
	 * is the one the square crop keeps whole.
	 */
	private static BufferedImage sample(ImageReader reader) throws IOException {
		int shorter = Math.min(reader.getWidth(0), reader.getHeight(0));
		int step = Math.max(1, shorter / (SIDE * 2));

		ImageReadParam parameters = reader.getDefaultReadParam();
		parameters.setSourceSubsampling(step, step, 0, 0);

		return reader.read(0, parameters);
	}

	/**
	 * Turns the sampled pixels the way the camera meant them to be seen.
	 *
	 * <p>A pixel loop rather than an {@code AffineTransform}, and deliberately:
	 * the eight cases are two coordinate expressions each, which can be read and
	 * checked one by one, where the equivalent transforms are four translate-scale
	 * -rotate incantations that are correct or not with nothing in between. The
	 * loop runs over the sampled image - a few hundred pixels a side - so the
	 * price of being obvious is a fraction of a millisecond.
	 */
	private static BufferedImage upright(BufferedImage source, int orientation) {
		if (orientation == JpegOrientation.UPRIGHT) {
			return source;
		}

		int width = source.getWidth();
		int height = source.getHeight();
		boolean turned = orientation >= JpegOrientation.FIRST_TURNED;

		BufferedImage upright = new BufferedImage(turned ? height : width,
				turned ? width : height, BufferedImage.TYPE_INT_RGB);

		for (int y = 0; y < upright.getHeight(); y++) {
			for (int x = 0; x < upright.getWidth(); x++) {
				upright.setRGB(x, y, source.getRGB(
						sourceX(orientation, x, y, width),
						sourceY(orientation, x, y, height)));
			}
		}

		return upright;
	}

	/** Orientation 2 and 3 mirror horizontally; 5 and 6 read a column as a row. */
	private static int sourceX(int orientation, int x, int y, int width) {
		return switch (orientation) {
			case 2, 3 -> width - 1 - x;
			case 5, 6 -> y;
			case 7, 8 -> width - 1 - y;
			default -> x;
		};
	}

	private static int sourceY(int orientation, int x, int y, int height) {
		return switch (orientation) {
			case 3, 4 -> height - 1 - y;
			case 5, 8 -> x;
			case 6, 7 -> height - 1 - x;
			default -> y;
		};
	}

	/**
	 * The middle square of the picture, scaled to the thumbnail's side.
	 *
	 * <p>A square cropped from the centre rather than the whole picture letterboxed
	 * into one: the circle on the card would show a car between two bars of
	 * background otherwise, and the middle of a photograph of a car is the car.
	 *
	 * <p>Drawn onto white first, because a PNG may be transparent and a JPEG has
	 * nowhere to put that - without the fill, transparent corners encode as black.
	 */
	private static BufferedImage square(BufferedImage source) {
		int side = Math.min(source.getWidth(), source.getHeight());
		int left = (source.getWidth() - side) / 2;
		int top = (source.getHeight() - side) / 2;

		BufferedImage thumbnail = new BufferedImage(SIDE, SIDE, BufferedImage.TYPE_INT_RGB);
		Graphics2D canvas = thumbnail.createGraphics();

		try {
			canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
					RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			canvas.setRenderingHint(RenderingHints.KEY_RENDERING,
					RenderingHints.VALUE_RENDER_QUALITY);
			canvas.setColor(Color.WHITE);
			canvas.fillRect(0, 0, SIDE, SIDE);
			canvas.drawImage(source, 0, 0, SIDE, SIDE,
					left, top, left + side, top + side, null);
		} finally {
			canvas.dispose();
		}

		return thumbnail;
	}

	private static byte[] encode(BufferedImage image) throws IOException {
		ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
		ImageWriteParam parameters = writer.getDefaultWriteParam();

		parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		parameters.setCompressionQuality(QUALITY);

		ByteArrayOutputStream bytes = new ByteArrayOutputStream();

		try (ImageOutputStream out = ImageIO.createImageOutputStream(bytes)) {
			writer.setOutput(out);
			writer.write(null, new IIOImage(image, null, null), parameters);
		} finally {
			writer.dispose();
		}

		return bytes.toByteArray();
	}
}
