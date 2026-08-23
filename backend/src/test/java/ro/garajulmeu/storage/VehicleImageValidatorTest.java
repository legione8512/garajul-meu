package ro.garajulmeu.storage;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import ro.garajulmeu.common.ImageInspection;
import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** No Spring. The checking is ImageInspectionTest's; what is tested here is the answer. */
class VehicleImageValidatorTest {

	/** Null R2: the validator never reads it, and the record turns it into an empty one. */
	private final VehicleImageValidator validator = new VehicleImageValidator(
			new StorageProperties("local", "./storage", 1024 * 1024, 200, 40_000_000, null));

	private static byte[] image(String format, int width, int height) throws IOException {
		BufferedImage picture = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(picture, format, out);
		return out.toByteArray();
	}

	private static ErrorCode codeThrownBy(VehicleImageValidator validator, byte[] bytes) {
		return assertThatThrownBy(() -> validator.accept(bytes))
				.isInstanceOf(ApiException.class)
				.extracting(thrown -> ((ApiException) thrown).errorCode())
				.asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.type(ErrorCode.class))
				.actual();
	}

	@Test
	void anAcceptedPhotographKeepsTheTypeMeasuredFromItsBytes() throws IOException {
		VehicleImage accepted = validator.accept(image("jpeg", 800, 400));

		assertThat(accepted.contentType()).isEqualTo("image/jpeg");
		assertThat(accepted.sizeBytes()).isEqualTo(accepted.bytes().length);
	}

	/**
	 * Every reason is mapped, and the mapping is asserted here rather than through
	 * seven fixtures - some of them, TOO_MANY_PIXELS in particular, need a
	 * handcrafted PNG header that ImageInspectionTest already builds. Looping the
	 * enum also means a reason added later fails this test as well as the compiler.
	 */
	@Test
	void everyRefusalReasonAnswersOneOfTheTwoCodesSectionSeventeenProvides() {
		for (ImageInspection.Refusal reason : ImageInspection.Refusal.values()) {
			assertThat(VehicleImageValidator.codeFor(reason))
					.as("%s", reason)
					.isIn(ErrorCode.IMAGE_TOO_LARGE, ErrorCode.IMAGE_INVALID_TYPE);
		}

		assertThat(VehicleImageValidator.codeFor(ImageInspection.Refusal.TOO_LARGE))
				.isEqualTo(ErrorCode.IMAGE_TOO_LARGE);
		assertThat(VehicleImageValidator.codeFor(ImageInspection.Refusal.TOO_MANY_PIXELS))
				.isEqualTo(ErrorCode.IMAGE_TOO_LARGE);
	}

	/**
	 * The distinction OCR deliberately does not make. A file too big is worth
	 * sending again smaller; a file that is not a photograph never will be.
	 */
	@Test
	void tooBigAndNotAPhotographAreDifferentAnswers() throws IOException {
		VehicleImageValidator tiny = new VehicleImageValidator(
				new StorageProperties("local", "./storage", 512, 200, 40_000_000, null));

		assertThat(codeThrownBy(tiny, image("png", 800, 400)))
				.isEqualTo(ErrorCode.IMAGE_TOO_LARGE);

		assertThat(codeThrownBy(validator, "MZ\u0090\u0000\u0003 not a car".getBytes()))
				.isEqualTo(ErrorCode.IMAGE_INVALID_TYPE);
	}

	/** GIF reads perfectly well and is refused by policy, which is still an invalid type. */
	@Test
	void aFormatWeDoNotKeepIsAnInvalidType() throws IOException {
		assertThat(codeThrownBy(validator, image("gif", 800, 400)))
				.isEqualTo(ErrorCode.IMAGE_INVALID_TYPE);
	}
}