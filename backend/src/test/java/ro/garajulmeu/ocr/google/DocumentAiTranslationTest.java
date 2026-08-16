package ro.garajulmeu.ocr.google;

import org.junit.jupiter.api.Test;

import com.google.cloud.documentai.v1.BoundingPoly;
import com.google.cloud.documentai.v1.Document;
import com.google.cloud.documentai.v1.NormalizedVertex;

import ro.garajulmeu.ocr.OcrDocument;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * No Spring, no network, no account. The adapter's risk is entirely in the
 * translation, and a hand-built response exercises it for nothing - which is
 * what let this be written before the Google project existed.
 *
 * <p>The text is the one Document AI actually sends: a single string for the
 * whole page, with every token pointing into it by index.
 */
class DocumentAiTranslationTest {

	/** Indices: A=[0,2) including its space, B=[2,3), 100=[4,7), ABC=[8,11). */
	private static final String PAGE_TEXT = "A B 100 ABC";

	private static NormalizedVertex vertex(float x, float y) {
		return NormalizedVertex.newBuilder().setX(x).setY(y).build();
	}

	private static Document.Page.Token.Builder token(int start, int end, float confidence) {
		return Document.Page.Token.newBuilder()
				.setLayout(Document.Page.Layout.newBuilder()
						.setConfidence(confidence)
						.setTextAnchor(Document.TextAnchor.newBuilder()
								.addTextSegments(Document.TextAnchor.TextSegment.newBuilder()
										.setStartIndex(start)
										.setEndIndex(end))));
	}

	private static Document.Page.Token positioned(int start, int end, float confidence,
			float left, float top, float right, float bottom) {
		Document.Page.Token.Builder builder = token(start, end, confidence);

		builder.getLayoutBuilder().setBoundingPoly(BoundingPoly.newBuilder()
				.addNormalizedVertices(vertex(left, top))
				.addNormalizedVertices(vertex(right, top))
				.addNormalizedVertices(vertex(right, bottom))
				.addNormalizedVertices(vertex(left, bottom)));

		return builder.build();
	}

	private static Document.Builder page(Document.Page.Token... tokens) {
		Document.Page.Builder page = Document.Page.newBuilder();
		for (Document.Page.Token token : tokens) {
			page.addTokens(token);
		}
		return Document.newBuilder().setText(PAGE_TEXT).addPages(page);
	}

	@Test
	void aTokenBecomesABlockCarryingItsTextConfidenceAndPosition() {
		OcrDocument document = DocumentAiTranslation.toOcrDocument(
				page(positioned(2, 3, 0.97f, 0.10f, 0.20f, 0.14f, 0.23f)).build());

		assertThat(document.blocks()).hasSize(1);

		OcrDocument.Block block = document.blocks().getFirst();
		assertThat(block.text()).isEqualTo("B");
		assertThat(block.confidence()).isEqualTo(0.97, within(0.0001));
		assertThat(block.box().x()).isEqualTo(0.10, within(0.0001));
		assertThat(block.box().width()).isEqualTo(0.04, within(0.0001));
		assertThat(block.box().height()).isEqualTo(0.03, within(0.0001));
	}

	/**
	 * The provider sends the page's text once and points into it. Reading the
	 * indices wrongly would produce plausible-looking words that belong to the
	 * neighbouring field, which is why two tokens are checked rather than one.
	 */
	@Test
	void theTextIsReadFromTheIndicesIntoTheDocumentsOwnText() {
		OcrDocument document = DocumentAiTranslation.toOcrDocument(page(
				positioned(4, 7, 0.9f, 0.20f, 0.20f, 0.24f, 0.23f),
				positioned(8, 11, 0.9f, 0.25f, 0.20f, 0.30f, 0.23f)).build());

		assertThat(document.blocks()).extracting(OcrDocument.Block::text)
				.containsExactly("100", "ABC");
	}

	/** A token's segment routinely includes the space that follows the word. */
	@Test
	void theSpaceATokenCarriesWithItIsNotPartOfItsText() {
		OcrDocument document = DocumentAiTranslation.toOcrDocument(
				page(positioned(0, 2, 0.9f, 0.02f, 0.20f, 0.05f, 0.23f)).build());

		assertThat(document.blocks().getFirst().text()).isEqualTo("A");
	}

	@Test
	void aTokenThatIsNothingButWhitespaceIsNotABlock() {
		OcrDocument document = DocumentAiTranslation.toOcrDocument(
				page(positioned(1, 2, 0.9f, 0.05f, 0.20f, 0.06f, 0.23f)).build());

		assertThat(document.blocks()).isEmpty();
	}

	/**
	 * Dropped rather than placed at the origin, where it would silently become the
	 * leftmost thing on the page and the mapper would offer it as somebody's VIN.
	 */
	@Test
	void aTokenWithNoNormalisedPositionIsDropped() {
		OcrDocument document = DocumentAiTranslation.toOcrDocument(
				page(token(2, 3, 0.9f).build()).build());

		assertThat(document.blocks()).isEmpty();
	}

	/**
	 * Boxes are normalised per page, so a second page's blocks would land on top
	 * of the first in coordinates that look perfectly valid.
	 */
	@Test
	void onlyTheFirstPageIsRead() {
		Document.Builder document = page(positioned(2, 3, 0.9f, 0.10f, 0.20f, 0.14f, 0.23f));
		document.addPages(Document.Page.newBuilder()
				.addTokens(positioned(4, 7, 0.9f, 0.10f, 0.20f, 0.14f, 0.23f)));

		assertThat(DocumentAiTranslation.toOcrDocument(document.build()).blocks())
				.extracting(OcrDocument.Block::text)
				.containsExactly("B");
	}

	@Test
	void aResponseWithNoPagesIsAnEmptyDocumentRatherThanAFailure() {
		assertThat(DocumentAiTranslation.toOcrDocument(Document.newBuilder().build()).blocks())
				.isEmpty();
	}
}