package ro.garajulmeu.ocr;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * How much OCR one account may use, and what an upload may be. Specification
 * sections 13 and 22.
 *
 * @param provider        {@code stub} answers with a canned document for local
 *                        development; {@code google} calls Document AI
 * @param dailyLimit      requests per account per calendar day
 * @param monthlyLimit    requests per account per calendar month
 * @param maxUploadBytes  the largest upload accepted, after the servlet
 *                        container has already refused anything far bigger
 * @param minSide         the smallest usable edge in pixels; below this there is
 *                        nothing legible to read and the request would spend an
 *                        allowance on a certain failure
 * @param maxPixels       a ceiling on width times height, checked from the
 *                        image header before anything is decoded - a small file
 *                        can declare an enormous canvas, and decoding it first
 *                        is how that becomes our problem rather than the
 *                        sender's
 */
@ConfigurationProperties(prefix = "garajul-meu.ocr")
public record OcrProperties(
		@DefaultValue("stub") String provider,
		int dailyLimit,
		int monthlyLimit,
		long maxUploadBytes,
		int minSide,
		int maxPixels) {

	public OcrProperties {
		dailyLimit = dailyLimit > 0 ? dailyLimit : 10;
		monthlyLimit = monthlyLimit > 0 ? monthlyLimit : 30;
		maxUploadBytes = maxUploadBytes > 0 ? maxUploadBytes : 10L * 1024 * 1024;
		minSide = minSide > 0 ? minSide : 200;
		maxPixels = maxPixels > 0 ? maxPixels : 40_000_000;

		// A monthly allowance below the daily one is a configuration mistake that
		// would look like a working system until somebody hit it, so it is
		// refused at startup rather than at the first request.
		if (monthlyLimit < dailyLimit) {
			throw new IllegalArgumentException(
					"The monthly OCR limit cannot be lower than the daily one");
		}
	}
}