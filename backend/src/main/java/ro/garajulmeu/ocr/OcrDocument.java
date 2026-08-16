package ro.garajulmeu.ocr;

import java.util.List;

/**
 * What an OCR provider found, in terms that name no provider.
 *
 * <p>Section 32 requires that business logic is not coupled to provider SDKs,
 * and section 13 that the mapper is provider-independent - so this is the whole
 * of what the rest of the application may know about a scan. Document AI's own
 * types stop at the adapter.
 *
 * <p>Boxes are fractions of the page, not pixels, because that is the only form
 * two providers could be expected to agree on and the only one that survives a
 * photograph being taken at a different size.
 */
public record OcrDocument(List<Block> blocks) {

	/**
	 * @param confidence between 0 and 1, as every provider reports it. Section 13
	 *                   combines this with semantic validation rather than
	 *                   trusting it alone
	 */
	public record Block(String text, double confidence, Box box) {
	}

	public record Box(double x, double y, double width, double height) {
	}
}