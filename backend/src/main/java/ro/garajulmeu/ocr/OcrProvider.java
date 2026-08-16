package ro.garajulmeu.ocr;

/**
 * The seam section 32 requires: {@code OcrProvider → GoogleDocumentAiOcrProvider}.
 *
 * <p>One method, taking an image that has already been checked and returning a
 * document that names no provider. Everything specific to Document AI - the
 * client, the processor name, the response types - lives behind this and nowhere
 * else, which is what lets the mapper in 9.2 and the screen in 9.4 be built and
 * tested with no Google account at all.
 *
 * <p>Implementations map their own failures to the section 17 codes:
 * OCR_PROVIDER_UNAVAILABLE when the service could not be reached,
 * OCR_PROCESSING_FAILED when it answered but the answer was unusable.
 */
public interface OcrProvider {

	OcrDocument read(OcrImage image);
}