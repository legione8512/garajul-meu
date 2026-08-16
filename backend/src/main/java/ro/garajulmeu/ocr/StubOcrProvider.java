package ro.garajulmeu.ocr;

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
 * <p>The blocks are shaped the way Document AI reports them - a printed code and
 * its value as separate pieces of text, each with a box and a confidence -
 * because 9.2's mapper anchors on the codes, and a stub that returned finished
 * field values would let the mapper be written against a convenience that the
 * real provider does not offer.
 *
 * <p>The confidences are deliberately uneven: one field below any sensible
 * threshold, so the review states in 9.4 have something to show that is not
 * simply "everything is fine".
 */
@Component
@ConditionalOnProperty(name = "garajul-meu.ocr.provider", havingValue = "stub")
public class StubOcrProvider implements OcrProvider {

	private static final Logger log = LoggerFactory.getLogger(StubOcrProvider.class);

	@Override
	public OcrDocument read(OcrImage image) {
		log.info("Stub OCR provider answering for a {} of {}x{}",
				image.contentType(), image.width(), image.height());

		return new OcrDocument(List.of(
				block("A", 0.99, 0.02, 0.05),
				block("B 100 ABC", 0.97, 0.05, 0.05),
				block("D.1", 0.99, 0.02, 0.13),
				block("Dacia", 0.96, 0.05, 0.13),
				block("D.3", 0.99, 0.02, 0.20),
				block("Logan", 0.94, 0.05, 0.20),
				block("E", 0.99, 0.02, 0.24),
				// Deliberately poor: a VIN is where a photograph most often fails.
				block("VF1AAAAAAAA00000I", 0.52, 0.05, 0.24),
				block("R", 0.99, 0.38, 0.23),
				block("albastru", 0.91, 0.42, 0.23)));
	}

	private static Block block(String text, double confidence, double x, double y) {
		return new Block(text, confidence, new Box(x, y, 0.1, 0.03));
	}
}