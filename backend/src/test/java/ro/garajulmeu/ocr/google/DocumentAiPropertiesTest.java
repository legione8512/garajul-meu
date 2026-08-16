package ro.garajulmeu.ocr.google;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The client itself cannot be tested without an account and a paid page, so what
 * is tested is everything around it that can be got wrong silently: the resource
 * name, the region, and a value nobody filled in.
 */
class DocumentAiPropertiesTest {

	private static final DocumentAiProperties CONFIGURED =
			new DocumentAiProperties("garajul-meu-505722", "eu", "7f0ef1c4beff0c0f");

	@Test
	void namesTheProcessorTheWayTheApiExpects() {
		assertThat(CONFIGURED.processorName())
				.isEqualTo("projects/garajul-meu-505722/locations/eu/processors/7f0ef1c4beff0c0f");
	}

	/**
	 * The console displays the region as EU and the API wants eu. Copying what is
	 * on screen is the natural thing to do, and it answers NOT_FOUND with no hint
	 * as to why - so the difference is absorbed here rather than discovered at
	 * two in the morning.
	 */
	@Test
	void theRegionIsLowerCasedBecauseTheConsoleShowsItInCapitals() {
		DocumentAiProperties asShownInTheConsole =
				new DocumentAiProperties("garajul-meu-505722", "EU", "7f0ef1c4beff0c0f");

		assertThat(asShownInTheConsole.processorName()).isEqualTo(CONFIGURED.processorName());
		assertThat(asShownInTheConsole.endpoint()).isEqualTo("eu-documentai.googleapis.com:443");
	}

	@Test
	void aMissingValueIsRefusedAndSaysWhichPropertyItWas() {
		DocumentAiProperties incomplete = new DocumentAiProperties("garajul-meu-505722", "eu", "  ");

		assertThatThrownBy(incomplete::processorName)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("garajul-meu.ocr.google.processor-id");
	}
}