package ro.garajulmeu.ocr;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import ro.garajulmeu.ocr.OcrDocument.Block;
import ro.garajulmeu.ocr.OcrDocument.Box;

/**
 * A canned reading of a Romanian registration certificate, for development and
 * for every test below the Google adapter. The counterpart of
 * LoggingEmailProvider, and active for the same reason: a whole phase can be
 * built and exercised before an account with a payment method exists.
 *
 * <p><strong>One block per word, because that is what Document AI returns.</strong>
 * The first version emitted a finished value per field, which let the mapper be
 * written against a convenience the real provider does not offer - a scan of a
 * real certificate arrives as tokens of one word each, and "B 100 ABC" is three
 * of them. The stub now says the same thing, so the joining in 9.3b is exercised
 * by every test that goes through the endpoint rather than only by unit tests.
 *
 * <p><strong>The geometry has to be possible, not merely present.</strong> An
 * earlier version gave every block the same tenth-of-a-page width, so a code's
 * own box swallowed the value printed beside it. Widths are therefore derived
 * from the number of characters, gaps from the distances measured on a real
 * certificate: a fifth of a code width from a code to its value, and a space
 * between words wide enough to be read as one.
 *
 * <p>The plate deliberately begins with B, which is also a printed code. That is
 * the ordinary case for a Bucharest certificate and the mapper has to survive
 * it: the reading is a plate, not a date.
 *
 * <p>The confidences are deliberately uneven: one field below any sensible
 * threshold, so the review states in 9.4 have something to show that is not
 * simply "everything is fine".
 */
@Component
@ConditionalOnProperty(name = "garajul-meu.ocr.provider", havingValue = "stub")
public class StubOcrProvider implements OcrProvider {

	private static final Logger log = LoggerFactory.getLogger(StubOcrProvider.class);

	/** One printed character, as a fraction of the page width. */
	private static final double CHARACTER = 0.012;

	/** Between two words of one value - read as a space, not as another cell. */
	private static final double SPACE = 0.005;

	/** Between a code and the value it labels. */
	private static final double CODE_GAP = 0.02;

	private static final double HEIGHT = 0.03;

	@Override
	public OcrDocument read(OcrImage image) {
		log.info("Stub OCR provider answering for a {} of {}x{}",
				image.contentType(), image.width(), image.height());

		return new Page()
				.row(0.02, 0.05).code("A").value(0.97, "B", "100", "ABC")
				.row(0.02, 0.12).code("D.1").value(0.96, "Dacia")
				.row(0.02, 0.19).code("D.3").value(0.94, "Logan")
				// Deliberately poor: a VIN is where a photograph most often fails.
				.row(0.02, 0.26).code("E").value(0.52, "VF1AAAAAAAA00000I")
				.row(0.38, 0.33).code("R").value(0.91, "albastru")
				.document();
	}

	/** Lays words out left to right, so the spacing is generated rather than typed. */
	private static final class Page {

		private final List<Block> blocks = new ArrayList<>();
		private double x;
		private double y;

		private Page row(double x, double y) {
			this.x = x;
			this.y = y;
			return this;
		}

		private Page code(String printed) {
			word(printed, 0.99);
			x += CODE_GAP;
			return this;
		}

		private Page value(double confidence, String... words) {
			for (String word : words) {
				word(word, confidence);
				x += SPACE;
			}
			return this;
		}

		private void word(String text, double confidence) {
			double width = text.length() * CHARACTER;
			blocks.add(new Block(text, confidence, new Box(x, y, width, HEIGHT)));
			x += width;
		}

		private OcrDocument document() {
			return new OcrDocument(List.copyOf(blocks));
		}
	}
}