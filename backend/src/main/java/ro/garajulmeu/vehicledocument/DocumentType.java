package ro.garajulmeu.vehicledocument;

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
	ROVINIETA
}