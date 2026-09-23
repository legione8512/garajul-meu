package ro.garajulmeu.vehiclepayment;

import java.util.Locale;
import java.util.Optional;

/**
 * What a recurring payment is for. Added in 1.1; the owner chose these two and
 * no others (docs/DESIGN_1_1_INSTALMENTS.md).
 *
 * <p>Text in the database rather than a CHECK constraint, as document types
 * are, so a third kind is a constant here and not a migration.
 */
public enum PaymentKind {

	/** A car loan or a leasing contract - one kind, because both are "the car's instalment". */
	LOAN,

	/** A CASCO policy paid in instalments, as distinct from the policy's own expiry. */
	CASCO;

	/** Case-insensitive, and empty rather than an exception for anything unknown. */
	public static Optional<PaymentKind> of(String raw) {
		if (raw == null) {
			return Optional.empty();
		}

		String cleaned = raw.trim().toUpperCase(Locale.ROOT);

		for (PaymentKind kind : values()) {
			if (kind.name().equals(cleaned)) {
				return Optional.of(kind);
			}
		}
		return Optional.empty();
	}
}
