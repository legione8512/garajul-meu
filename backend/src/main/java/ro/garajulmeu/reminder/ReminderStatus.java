package ro.garajulmeu.reminder;

/**
 * The life of one reminder. Specification section 10.6.
 *
 * <p>CANCELLED rather than deletion, throughout: section 12 has reminders
 * cancelled when a period changes or a record is superseded, and a row that
 * explains why nothing arrived is worth more than an absence that explains
 * nothing.
 */
public enum ReminderStatus {

	/** Scheduled and not yet claimed. The only state the scheduler looks for. */
	PENDING,

	/** Claimed by the scheduler. Guards against a second pass picking it up again. */
	PROCESSING,

	SENT,

	/** Attempted and refused by the provider. Retryable; keeps its error code. */
	FAILED,

	/** Superseded by a changed period, a renewal, or a preference turned off. */
	CANCELLED
}