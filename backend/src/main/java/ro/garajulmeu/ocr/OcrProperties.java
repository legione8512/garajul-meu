package ro.garajulmeu.ocr;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * How much OCR one account may use, what an upload may be, and how sure the
 * provider has to be. Specification sections 13 and 22.
 *
 * @param provider            {@code stub} answers with a canned document for
 *                            local development; {@code google} calls Document AI
 * @param dailyLimit          requests per account per calendar day
 * @param monthlyLimit        requests per account per calendar month
 * @param maxUploadBytes      the largest upload accepted, after the servlet
 *                            container has already refused anything far bigger
 * @param minSide             the smallest usable edge in pixels
 * @param maxPixels           a ceiling on width times height, checked from the
 *                            image header before anything is decoded
 * @param confidenceThreshold below this a field is marked for review rather than
 *                            accepted. <strong>Provisional.</strong> Section 35
 *                            says the real thresholds are calibrated against
 *                            representative samples, in the same words it uses
 *                            for the template coordinates - and there are no
 *                            samples yet, so this number is a placeholder that
 *                            says so
 */
@ConfigurationProperties(prefix = "garajul-meu.ocr")
public record OcrProperties(
		@DefaultValue("stub") String provider,
		int dailyLimit,
		int monthlyLimit,
		long maxUploadBytes,
		int minSide,
		int maxPixels,
		double confidenceThreshold) {

	public OcrProperties {
		dailyLimit = dailyLimit > 0 ? dailyLimit : 10;
		monthlyLimit = monthlyLimit > 0 ? monthlyLimit : 30;
		maxUploadBytes = maxUploadBytes > 0 ? maxUploadBytes : 10L * 1024 * 1024;
		minSide = minSide > 0 ? minSide : 200;
		maxPixels = maxPixels > 0 ? maxPixels : 40_000_000;
		confidenceThreshold = confidenceThreshold > 0 ? confidenceThreshold : 0.80;

		if (monthlyLimit < dailyLimit) {
			throw new IllegalArgumentException(
					"The monthly OCR limit cannot be lower than the daily one");
		}
		if (confidenceThreshold > 1) {
			throw new IllegalArgumentException(
					"An OCR confidence threshold above 1 can never be met");
		}
	}
}