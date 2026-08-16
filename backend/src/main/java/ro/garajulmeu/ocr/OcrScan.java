package ro.garajulmeu.ocr;

import java.util.List;

/**
 * What a scan proposes. Nothing here is saved: section 16 describes the endpoint
 * as "Multipart OCR; no vehicle save", and section 13 requires that the user
 * reviews and corrects before anything is stored.
 *
 * <p>Every certificate field appears exactly once, including the ones nothing
 * was found for. Section 7 asks the screen to show "detected", "needs review"
 * and "not detected", and a field left out of the answer would leave the screen
 * guessing which of the three it was.
 */
public record OcrScan(List<ProposedField> fields) {

	/**
	 * @param value      always in the form the field can hold - an ISO date for a
	 *                   date field, digits for a number - or null when nothing
	 *                   usable was read. A certificate prints 14.03.2019 and a
	 *                   date input wants 2019-03-14; proposing the printed form
	 *                   would put text on screen that the field cannot display
	 * @param confidence what the provider reported for the text this came from,
	 *                   or 0 when nothing was found
	 */
	public record ProposedField(String field, String value, double confidence, FieldStatus status) {
	}
}