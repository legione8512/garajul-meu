package ro.garajulmeu.ocr;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * How much OCR one account may use. Specification section 13 sets the initial
 * default at ten requests per day and thirty per month, and says the production
 * quota is configurable and that development may use different limits.
 *
 * @param dailyLimit   requests per account per calendar day
 * @param monthlyLimit requests per account per calendar month
 */
@ConfigurationProperties(prefix = "garajul-meu.ocr")
public record OcrProperties(int dailyLimit, int monthlyLimit) {

	public OcrProperties {
		dailyLimit = dailyLimit > 0 ? dailyLimit : 10;
		monthlyLimit = monthlyLimit > 0 ? monthlyLimit : 30;

		// A monthly allowance below the daily one is a configuration mistake that
		// would look like a working system until somebody hit it, so it is
		// refused at startup rather than at the first request.
		if (monthlyLimit < dailyLimit) {
			throw new IllegalArgumentException(
					"The monthly OCR limit cannot be lower than the daily one");
		}
	}
}