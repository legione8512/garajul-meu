package ro.garajulmeu.reminder;

import java.time.LocalDate;
import java.util.UUID;

import ro.garajulmeu.user.Language;
import ro.garajulmeu.vehicledocument.DocumentType;

/**
 * One reminder that is due, with everything needed to send it, fetched in one
 * round trip.
 *
 * <p>A projection rather than the entity because sending a reminder needs five
 * tables - the reminder, its document, the vehicle, the certificate that holds
 * the plate, and the account that holds the language. Loading entities and
 * navigating would be five queries per reminder, and at nine in the morning the
 * whole user base is due at once.
 *
 * @param vehicleLabel what the owner named the car, falling back to its
 *                     registration plate. Never the VIN: section 18 keeps it off
 *                     a lock screen.
 */
public record DueReminder(UUID reminderId, int offsetDays, UUID userId, UUID vehicleId,
		UUID documentId, DocumentType type, LocalDate validUntil, String vehicleLabel,
		Language language) {

	public ReminderMessage.Subject subject() {
		return new ReminderMessage.Subject(vehicleId, documentId, type, validUntil, vehicleLabel);
	}
}