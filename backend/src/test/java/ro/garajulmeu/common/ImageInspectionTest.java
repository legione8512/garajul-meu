package ro.garajulmeu.common;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * No Spring, no error codes: this is arithmetic on bytes and a header.
 *
 * <p>These cases were {@code OcrImageValidatorTest}'s until 12.1, and they moved
 * rather than being copied - the checking is one rule and belongs to one test.
 * What stayed behind is the part that is genuinely about OCR: that every one of
 * these findings answers the same code.
 */
class ImageInspectionTest {

	private static final ImageInspection.Limits LIMITS =
			new ImageInspection.Limits(1024 * 1024, 40_000_000, 200);

	private static byte[] image(String format, int width, int height) throws IOException {
		BufferedImage picture = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(picture, format, out);
		return out.toByteArray();
	}

	private static ImageInspection.Refusal refusalOf(byte[] bytes,
			ImageInspection.Limits limits) {
		ImageInspection.Result result = ImageInspection.inspect(bytes, limits);

		assertThat(result).isInstanceOf(ImageInspection.Refused.class);
		return ((ImageInspection.Refused) result).reason();
	}

	@Test
	void aPhotographIsAcceptedAndMeasured() throws IOException {
		ImageInspection.Result result = ImageInspection.inspect(image("jpeg", 800, 400), LIMITS);

		assertThat(result).isInstanceOf(ImageInspection.Accepted.class);

		ImageInspection.Accepted accepted = (ImageInspection.Accepted) result;
		assertThat(accepted.contentType()).isEqualTo("image/jpeg");
		assertThat(accepted.width()).isEqualTo(800);
		assertThat(accepted.height()).isEqualTo(400);
	}

	/**
	 * The point of reading the format from the bytes. Nothing here is told what
	 * the client called the file, and a PNG is a PNG however it was announced.
	 */
	@Test
	void theFormatComesFromTheBytesAndNotFromAnyDeclaration() throws IOException {
		ImageInspection.Result result = ImageInspection.inspect(image("png", 800, 400), LIMITS);

		assertThat(((ImageInspection.Accepted) result).contentType()).isEqualTo("image/png");
	}

	@Test
	void somethingThatIsNotAnImageIsRefusedAsSuch() {
		assertThat(refusalOf("MZ\u0090\u0000\u0003 not a photograph".getBytes(), LIMITS))
				.isEqualTo(ImageInspection.Refusal.NOT_AN_IMAGE);
	}

	/**
	 * A distinction the old validator could not make, and 12.1 is why it can now.
	 * A GIF is a perfectly good image that ImageIO reads without complaint - it is
	 * refused by policy, not by failure, and the two are different findings even
	 * though OCR still answers them identically.
	 */
	@Test
	void aRealImageInAFormatWeDoNotKeepIsToldApartFromRubbish() throws IOException {
		assertThat(refusalOf(image("gif", 800, 400), LIMITS))
				.isEqualTo(ImageInspection.Refusal.UNSUPPORTED_FORMAT);
	}

	@Test
	void anEmptyUploadIsRefused() {
		assertThat(refusalOf(new byte[0], LIMITS)).isEqualTo(ImageInspection.Refusal.EMPTY);
	}

	@Test
	void anUploadOverTheLimitIsRefusedOnItsSize() throws IOException {
		ImageInspection.Limits tiny = new ImageInspection.Limits(512, 40_000_000, 200);

		assertThat(refusalOf(image("png", 800, 400), tiny))
				.isEqualTo(ImageInspection.Refusal.TOO_LARGE);
	}

	@Test
	void anImageBelowTheSmallestUsefulEdgeIsRefused() throws IOException {
		assertThat(refusalOf(image("png", 80, 40), LIMITS))
				.isEqualTo(ImageInspection.Refusal.TOO_SMALL);
	}

	/**
	 * The decompression bomb. This is a valid PNG header declaring a canvas of
	 * twenty thousand pixels a side in a handful of bytes; the guard reads the
	 * dimensions from the header and refuses before anything is decoded, which is
	 * the difference between rejecting a file and allocating 1.6 gigabytes to
	 * find out we should have.
	 */
	@Test
	void aHeaderDeclaringAnEnormousCanvasIsRefusedBeforeDecoding() {
		byte[] ihdr = ByteBuffer.allocate(17)
				.put("IHDR".getBytes())
				.putInt(20_000).putInt(20_000)
				.put((byte) 8).put((byte) 2).put((byte) 0).put((byte) 0).put((byte) 0)
				.array();

		CRC32 crc = new CRC32();
		crc.update(ihdr);

		byte[] png = ByteBuffer.allocate(8 + 4 + 17 + 4)
				.put(new byte[] { (byte) 137, 80, 78, 71, 13, 10, 26, 10 })
				.putInt(13)
				.put(ihdr)
				.putInt((int) crc.getValue())
				.array();

		assertThat(refusalOf(png, LIMITS)).isEqualTo(ImageInspection.Refusal.TOO_MANY_PIXELS);
	}
}