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
 * <p><strong>The geometry has to be possible, not merely present.</strong> The
 * first version gave every block the same tenth-of-a-page width, so a code's own
 * box swallowed the value printed beside it and the mapper - correctly - refused
 * to read a value from inside its anchor. It also stacked the rows one line
 * tolerance apart, which let a value on one row answer for a code on the next.
 * Codes are therefore narrow, values start clear of them, and rows are spaced
 * well beyond the tolerance.
 *
 * <p>The confidences are deliberately uneven: one field below any sensible
 * threshold, so the review states in 9.4 have something to show that is not
 * simply "everything is fine".
 */
@Component
@ConditionalOnProperty(name = "garajul-meu.ocr.provider", havingValue = "stub")
public class StubOcrProvider implements OcrProvider {

	private static final Logger log = LoggerFactory.getLogger(StubOcrProvider.class);

	/** A printed code is a couple of characters wide, never a tenth of the page. */
	private static final double CODE_WIDTH = 0.03;
	private static final double VALUE_WIDTH = 0.20;
	private static final double HEIGHT = 0.03;

	@Override
	public OcrDocument read(OcrImage image) {
		log.info("Stub OCR provider answering for a {} of {}x{}",
				image.contentType(), image.width(), image.height());

		return new OcrDocument(List.of(
				code("A", 0.99, 0.02, 0.05),
				value("B 100 ABC", 0.97, 0.10, 0.05),
				code("D.1", 0.99, 0.02, 0.12),
				value("Dacia", 0.96, 0.10, 0.12),
				code("D.3", 0.99, 0.02, 0.19),
				value("Logan", 0.94, 0.10, 0.19),
				code("E", 0.99, 0.02, 0.26),
				// Deliberately poor: a VIN is where a photograph most often fails.
				value("VF1AAAAAAAA00000I", 0.52, 0.10, 0.26),
				code("R", 0.99, 0.38, 0.33),
				value("albastru", 0.91, 0.45, 0.33)));
	}

	private static Block code(String text, double confidence, double x, double y) {
		return new Block(text, confidence, new Box(x, y, CODE_WIDTH, HEIGHT));
	}

	private static Block value(String text, double confidence, double x, double y) {
		return new Block(text, confidence, new Box(x, y, VALUE_WIDTH, HEIGHT));
	}
}