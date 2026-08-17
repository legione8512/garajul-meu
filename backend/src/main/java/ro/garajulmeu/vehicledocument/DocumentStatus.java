package ro.garajulmeu.vehicledocument;

/**
 * What a document's remaining time means. Specification section 11.
 *
 * <p><strong>None of these is stored.</strong> Every one of them is a statement
 * about a document *and a date*, and the date is the reader's today - so the
 * same row is ACTIVE in the morning and EXPIRING_SOON thirty-one days later
 * without anything having been written. Persisting one would be persisting the
 * answer to a question nobody asked yet.
 */
public enum DocumentStatus {

	/** Thirty-one days or more remaining. */
	ACTIVE,

	/** Eight to thirty days. */
	EXPIRING_SOON,

	/** One to seven days. */
	URGENT,

	/** The last day of validity, which is still a valid day. */
	EXPIRES_TODAY,

	/** The day after the last valid one, or later. */
	EXPIRED,

	/**
	 * No record of this type has ever existed for the vehicle. Section 11 calls
	 * this a presentation state and says explicitly that it is never persisted on
	 * a VehicleDocument - it is the absence of one.
	 */
	NOT_CONFIGURED
}