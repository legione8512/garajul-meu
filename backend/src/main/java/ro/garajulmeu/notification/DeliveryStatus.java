package ro.garajulmeu.notification;

/**
 * What became of one message to one device. Specification section 10.8.
 *
 * <p><strong>One state short of {@link ro.garajulmeu.reminder.ReminderStatus},
 * and the missing one is PROCESSING.</strong> A delivery is never claimed on its
 * own: the scheduler claims the reminder, and every delivery underneath belongs
 * to that claim for as long as it lasts. Giving deliveries their own in-flight
 * state would be a second thing to leave stranded by a crash, recovering nothing
 * the reminder's claim does not already recover.
 */
public enum DeliveryStatus {

	/** Written before the attempt, so a crash mid-send leaves evidence behind. */
	PENDING,

	/** The provider accepted it. Not "the phone showed it" - nobody can know that. */
	SENT,

	/** The provider refused it. Keeps the provider's code and nothing else. */
	FAILED,

	/** The reminder was cancelled, or the device stopped accepting notifications. */
	CANCELLED
}