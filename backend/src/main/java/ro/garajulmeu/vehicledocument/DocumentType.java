package ro.garajulmeu.vehicledocument;

import java.util.Locale;
import java.util.Optional;

/**
 * What the application tracks an expiry date for. Specification sections 1 and
 * 10.4 named the first four.
 *
 * <p>Stored as text, so the name of a constant is a database value and renaming
 * one is a migration rather than a refactoring. Adding one is neither: V6 chose
 * text over a CHECK constraint because "the set will grow".
 *
 * <p><strong>It grew on 2026-09-22, in version 1.0.2, by the owner's decision</strong>
 * - a deliberate step past the four the specification names. The fire
 * extinguisher and the first-aid kit are compulsory equipment in Romania with an
 * expiry date of their own, checked at the ITP and at the roadside, and they fit
 * a document's shape exactly: a period that ends, and reminders before it does.
 *
 * <p>{@code alwaysShown} is the difference between the two groups, and it is a
 * presentation decision the owner made: the dashboard lists the first four for
 * every vehicle, configured or not, because not having an RCA is itself news;
 * the equipment appears only once somebody has entered it, so that a garage of
 * people who do not track their extinguisher does not fill with lines saying so.
 */
public enum DocumentType {

	/** Mandatory third-party liability insurance. */
	RCA(true),

	/** Optional comprehensive insurance. */
	CASCO(true),

	/** Periodic technical inspection - the date is the *next* inspection. */
	ITP(true),

	/** Road tax. */
	ROVINIETA(true),

	/** The fire extinguisher's expiry or next check - since 1.0.2. */
	EXTINGUISHER(false),

	/** The first-aid kit's expiry - since 1.0.2. */
	FIRST_AID_KIT(false);

	private final boolean alwaysShown;

	DocumentType(boolean alwaysShown) {
		this.alwaysShown = alwaysShown;
	}

	/**
	 * Whether the dashboard lists this type for a vehicle that has none of it -
	 * as NOT_CONFIGURED - or only once a record of it exists.
	 */
	public boolean alwaysShown() {
		return alwaysShown;
	}

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
