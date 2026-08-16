package ro.garajulmeu.registrationcertificate;

import java.util.Locale;

/**
 * How certificate values are normalised before they are stored.
 *
 * <p>Shared rather than duplicated, because both paths that write a VIN - adding
 * a vehicle and correcting its certificate - must agree exactly. Two copies of
 * this logic would be the one thing that lets the same car into the same garage
 * twice: the unique index compares bytes, so a difference of one space between
 * the two paths is enough.
 */
public final class CertificateValues {

	private CertificateValues() {
	}

	/**
	 * A VIN contains no spaces and no lower case, so both are removed rather than
	 * refused. Section 13 puts semantic VIN validation inside the OCR review;
	 * this is only about making equal values compare equal.
	 */
	public static String normalisedVin(String raw) {
		return raw.replaceAll("\\s", "").toUpperCase(Locale.ROOT);
	}

	/**
	 * Registration numbers are uppercase by law, and the garage is ordered by
	 * this column - leaving the case alone would sort a row away from its
	 * neighbours for no reason the reader could see.
	 */
	public static String normalisedRegistrationNumber(String raw) {
		return raw.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
	}

	/** Blank means absent. Make and description keep their case: "Dacia" is not "DACIA". */
	public static String trimmedOrNull(String raw) {
		if (raw == null) {
			return null;
		}
		String trimmed = raw.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}