package ro.garajulmeu.vehicledocument;

import java.util.Locale;
import java.util.Optional;

/**
 * The four documents V1 tracks. Specification sections 1 and 10.4.
 *
 * <p>Stored as text, so the name of a constant is a database value and renaming
 * one is a migration rather than a refactoring.
 */
public enum DocumentType {

	/** Mandatory third-party liability insurance. */
	RCA,

	/** Optional comprehensive insurance. */
	CASCO,

	/** Periodic technical inspection - the date is the *next* inspection. */
	ITP,

	/** Road tax. */
	ROVINIETA;

	/**
	 * Reads a type from what a client sent, or nothing at all.
	 *
	 * <p>Answers an {@code Optional} rather than throwing, in the shape of
	 * {@code CertificateCode.ofPrinted}: the enum stays free of web concerns and
	 * the service decides what an unreadable type means. The request carries this
	 * as text for exactly that reason - taking the enum directly would let Jackson
	 * refuse it first, and a client would be told its whole request was malformed
	 * when one named field was wrong.
	 */
	public static Optional<DocumentType> of(String raw) {
		if (raw == null) {
			return Optional.empty();
		}

		String cleaned = raw.trim().toUpperCase(Locale.ROOT);

		for (DocumentType type : values()) {
			if (type.name().equals(cleaned)) {
				return Optional.of(type);
			}
		}
		return Optional.empty();
	}
}