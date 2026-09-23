package ro.garajulmeu.reminder;

import java.time.LocalDate;
import java.util.UUID;

import ro.garajulmeu.push.PushNotification;
import ro.garajulmeu.user.Language;
import ro.garajulmeu.vehicledocument.DocumentType;
import ro.garajulmeu.vehiclepayment.PaymentKind;

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
 * <p><strong>Since 1.1 it is about a document or about one instalment of a
 * recurring payment</strong>, and exactly one of {@code documentId} and
 * {@code paymentId} is set - the same rule V13 puts on the reminder row. Each
 * repository query uses the constructor for its own kind.
 *
 * @param date         the document's {@code valid_until}, or the instalment's
 *                     due date
 * @param vehicleLabel what the owner named the car, falling back to its
 *                     registration plate. Never the VIN: section 18 keeps it off
 *                     a lock screen.
 */
public record DueReminder(UUID reminderId, int offsetDays, UUID userId, UUID vehicleId,
		UUID documentId, DocumentType type, LocalDate date, String vehicleLabel,
		Language language, UUID paymentId, PaymentKind paymentKind) {

	/** A document's reminder: {@code ReminderRepository.findDue}. */
	public DueReminder(UUID reminderId, int offsetDays, UUID userId, UUID vehicleId,
			UUID documentId, DocumentType type, LocalDate validUntil, String vehicleLabel,
			Language language) {
		this(reminderId, offsetDays, userId, vehicleId, documentId, type, validUntil, vehicleLabel,
				language, null, null);
	}

	/** An instalment's reminder: {@code ReminderRepository.findDuePayments}. */
	public DueReminder(UUID reminderId, int offsetDays, UUID userId, UUID vehicleId,
			UUID paymentId, PaymentKind paymentKind, LocalDate dueDate, String vehicleLabel,
			Language language) {
		this(reminderId, offsetDays, userId, vehicleId, null, null, dueDate, vehicleLabel,
				language, paymentId, paymentKind);
	}

	public boolean isPayment() {
		return paymentId != null;
	}

	/** The words for the lock screen, whichever kind of reminder this is. */
	public PushNotification notification() {
		if (isPayment()) {
			return ReminderMessage.forInstalment(new ReminderMessage.Instalment(vehicleId, paymentId,
					paymentKind, date, vehicleLabel), offsetDays, language);
		}
		return ReminderMessage.forSubject(subject(), offsetDays, language);
	}

	public ReminderMessage.Subject subject() {
		return new ReminderMessage.Subject(vehicleId, documentId, type, date, vehicleLabel);
	}
}
