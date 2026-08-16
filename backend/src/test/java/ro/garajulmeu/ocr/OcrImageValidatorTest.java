package ro.garajulmeu.ocr;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * No Spring at all. The validator takes its limits from a record, so it can be
 * built directly and every case runs in microseconds.
 */
class OcrImageValidatorTest {

	private final OcrImageValidator validator =
			new OcrImageValidator(new OcrProperties("stub", 10, 30, 1024 * 1024, 200, 40_000_000, 0.80));
	private static byte[] image(String format, int width, int height) throws IOException {
		BufferedImage picture = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(picture, format, out);
		return out.toByteArray();
	}

	private static void assertRefused(byte[] bytes, OcrImageValidator validator) {
		assertThatThrownBy(() -> validator.accept(bytes))
				.isInstanceOf(ApiException.class)
				.extracting(thrown -> ((ApiException) thrown).errorCode())
				.isEqualTo(ErrorCode.OCR_FILE_INVALID);
	}

	@Test
	void aPhotographIsAcceptedAndMeasured() throws IOException {
		OcrImage accepted = validator.accept(image("jpeg", 800, 400));

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
		assertThat(validator.accept(image("png", 800, 400)).contentType()).isEqualTo("image/png");
	}

	@Test
	void somethingThatIsNotAnImageIsRefused() {
		assertRefused("MZ\u0090\u0000\u0003 not a photograph".getBytes(), validator);
	}

	@Test
	void anEmptyUploadIsRefused() {
		assertRefused(new byte[0], validator);
	}

	@Test
	void anUploadOverTheLimitIsRefused() throws IOException {
		OcrImageValidator tiny =
				new OcrImageValidator(new OcrProperties("stub", 10, 30, 512, 200, 40_000_000, 0.80));
		assertRefused(image("png", 800, 400), tiny);
	}

	/** Below this there is nothing legible, and the request would spend an allowance to learn that. */
	@Test
	void anImageTooSmallToReadIsRefused() throws IOException {
		assertRefused(image("png", 80, 40), validator);
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

		assertRefused(png, validator);
	}
}