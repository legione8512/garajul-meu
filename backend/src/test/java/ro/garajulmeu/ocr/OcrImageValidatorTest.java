package ro.garajulmeu.ocr;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * What is left after 12.1 moved the checking to {@code ImageInspectionTest}:
 * that this validator carries the limits from its own properties, and that it
 * answers one code for everything.
 *
 * <p>The second half is the test worth having. Four genuinely different findings
 * are flattened into OCR_FILE_INVALID on purpose, and a flattening nobody
 * asserts is indistinguishable from a flattening nobody noticed.
 */
class OcrImageValidatorTest {

	private final OcrImageValidator validator = new OcrImageValidator(
			new OcrProperties("stub", 10, 30, 1024 * 1024, 200, 40_000_000, 0.80));

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
	void anAcceptedPhotographKeepsTheTypeAndSizeThatWereMeasured() throws IOException {
		OcrImage accepted = validator.accept(image("jpeg", 800, 400));

		assertThat(accepted.contentType()).isEqualTo("image/jpeg");
		assertThat(accepted.width()).isEqualTo(800);
		assertThat(accepted.height()).isEqualTo(400);
	}

	/**
	 * Rubbish, an empty part, a picture in a format we do not keep and a thumbnail
	 * are four findings and one answer. The advice is the same in every case -
	 * send a photograph of the certificate - and naming which check objected would
	 * describe our validator rather than their problem.
	 */
	@Test
	void everyKindOfRefusalAnswersTheSameCode() throws IOException {
		assertRefused(new byte[0], validator);
		assertRefused("MZ\u0090\u0000\u0003 not a photograph".getBytes(), validator);
		assertRefused(image("gif", 800, 400), validator);
		assertRefused(image("png", 80, 40), validator);
	}

	/** The limit comes from this validator's own properties and not from a constant. */
	@Test
	void theSizeLimitIsTheOneTheseParticularPropertiesCarry() throws IOException {
		byte[] photograph = image("png", 800, 400);

		assertThat(validator.accept(photograph)).isNotNull();

		OcrImageValidator tiny = new OcrImageValidator(
				new OcrProperties("stub", 10, 30, 512, 200, 40_000_000, 0.80));

		assertRefused(photograph, tiny);
	}
}