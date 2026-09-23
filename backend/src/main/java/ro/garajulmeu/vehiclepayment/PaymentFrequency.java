package ro.garajulmeu.vehiclepayment;

import java.util.Locale;
import java.util.Optional;

/** How often an instalment falls due, as a number of calendar months. */
public enum PaymentFrequency {

	MONTHLY(1),

	QUARTERLY(3),

	SEMIANNUAL(6),

	ANNUAL(12);

	private final int months;

	PaymentFrequency(int months) {
		this.months = months;
	}

	public int months() {
		return months;
	}

	/** Case-insensitive, and empty rather than an exception for anything unknown. */
	public static Optional<PaymentFrequency> of(String raw) {
		if (raw == null) {
			return Optional.empty();
		}

		String cleaned = raw.trim().toUpperCase(Locale.ROOT);

		for (PaymentFrequency frequency : values()) {
			if (frequency.name().equals(cleaned)) {
				return Optional.of(frequency);
			}
		}
		return Optional.empty();
	}
}
