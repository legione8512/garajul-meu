package ro.garajulmeu.ocr.google;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.cloud.documentai.v1.Document;

import ro.garajulmeu.ocr.OcrDocument;

/**
 * Document AI's answer, in terms that name no provider.
 *
 * <p>This is the whole of what section 32's seam actually costs: one file that
 * knows both vocabularies. Everything above it - the mapper, the quota, the
 * screen - sees only {@link OcrDocument}, which is why 9.2 could be built and
 * tested before this class existed.
 *
 * <p><strong>Tokens, not lines or paragraphs.</strong> A token is one word, and
 * a word is the smallest thing the provider will report a position and a
 * confidence for. Coarser granularity would fuse a printed code with the value
 * beside it and leave the mapper nothing to anchor on; finer does not exist.
 * The cost of this choice is that a value written as several words arrives as
 * several blocks, which is the reassembly 9.3b owes.
 *
 * <p>Pure and static on purpose: no client, no network, no credentials. The
 * risk in an adapter is the translation, and this way the translation is tested
 * without an account and without paying for a page.
 */
final class DocumentAiTranslation {

	private DocumentAiTranslation() {
	}

	/**
	 * <p>Only the first page is read. Section 22 accepts JPEG and PNG and nothing
	 * else, so a scan is always one page; and because boxes are normalised
	 * <em>per page</em>, blocks from a second page would land on top of the first
	 * in coordinates that look valid and mean nothing.
	 */
	static OcrDocument toOcrDocument(Document document) {
		if (document.getPagesCount() == 0) {
			return new OcrDocument(List.of());
		}

		String text = document.getText();
		List<OcrDocument.Block> blocks = new ArrayList<>();

		for (Document.Page.Token token : document.getPages(0).getTokensList()) {
			blockOf(token.getLayout(), text).ifPresent(blocks::add);
		}

		return new OcrDocument(List.copyOf(blocks));
	}

	private static Optional<OcrDocument.Block> blockOf(Document.Page.Layout layout, String documentText) {
		String text = textOf(layout, documentText);

		if (text.isEmpty()) {
			return Optional.empty();
		}

		// A block with no position is of no use to a mapper that reasons entirely
		// about where things sit, so it is dropped rather than placed at the
		// origin - where it would silently become the leftmost thing on the page.
		return boxOf(layout).map(box -> new OcrDocument.Block(text, layout.getConfidence(), box));
	}

	/**
	 * The provider does not repeat the text inside each token; it reports indices
	 * into the document's single text field, and a token's segment routinely
	 * includes the space or newline that follows the word - hence the trim.
	 */
	private static String textOf(Document.Page.Layout layout, String documentText) {
		StringBuilder text = new StringBuilder();

		for (var segment : layout.getTextAnchor().getTextSegmentsList()) {
			int start = (int) segment.getStartIndex();
			int end = (int) Math.min(segment.getEndIndex(), documentText.length());

			if (start >= 0 && start < end) {
				text.append(documentText, start, end);
			}
		}

		return text.toString().trim();
	}

	/**
	 * The enclosing rectangle of the reported polygon.
	 *
	 * <p>Document AI answers with four vertices, which on a photograph taken by
	 * hand are a rotated quadrilateral rather than a rectangle. The mapper reasons
	 * in rows and columns, so the axis-aligned box that contains the shape is the
	 * right reduction - and the error it introduces grows with the tilt, which is
	 * one more reason 9.5 calibrates against real photographs.
	 */
	private static Optional<OcrDocument.Box> boxOf(Document.Page.Layout layout) {
		var vertices = layout.getBoundingPoly().getNormalizedVerticesList();

		if (vertices.isEmpty()) {
			return Optional.empty();
		}

		float left = Float.MAX_VALUE;
		float top = Float.MAX_VALUE;
		float right = -Float.MAX_VALUE;
		float bottom = -Float.MAX_VALUE;

		for (var vertex : vertices) {
			left = Math.min(left, vertex.getX());
			top = Math.min(top, vertex.getY());
			right = Math.max(right, vertex.getX());
			bottom = Math.max(bottom, vertex.getY());
		}

		return Optional.of(new OcrDocument.Box(left, top, right - left, bottom - top));
	}
}