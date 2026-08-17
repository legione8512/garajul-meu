package ro.garajulmeu.reminder;

import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;
import ro.garajulmeu.notification.NotificationPreferences;
import ro.garajulmeu.notification.NotificationPreferencesService;
import ro.garajulmeu.user.User;
import ro.garajulmeu.user.UserRepository;
import ro.garajulmeu.vehicledocument.VehicleDocument;
import ro.garajulmeu.vehicledocument.VehicleDocumentRepository;

/**
 * Keeps the reminders of a document agreeing with the document.
 *
 * <p><strong>This package and {@code vehicledocument} depend on each other, and
 * that was chosen rather than tolerated.</strong> A document's period decides its
 * reminders, so the coupling is real; the alternative was a Spring application
 * event, which would break the cycle on a diagram while adding transaction
 * phases, ordering and an indirection that makes "what happens when I save a
 * document" unanswerable by reading the method that saves it. Java has no
 * objection to the cycle, and the explicitness is worth more than the arrow.
 */
@Service
public class ReminderService {

	private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

	private final ReminderRepository reminderRepository;
	private final VehicleDocumentRepository documentRepository;
	private final NotificationPreferencesService preferencesService;
	private final UserRepository userRepository;
	private final Clock clock;

	ReminderService(ReminderRepository reminderRepository,
			VehicleDocumentRepository documentRepository,
			NotificationPreferencesService preferencesService,
			UserRepository userRepository, Clock clock) {
		this.reminderRepository = reminderRepository;
		this.documentRepository = documentRepository;
		this.preferencesService = preferencesService;
		this.userRepository = userRepository;
		this.clock = clock;
	}

	/**
	 * Cancels what this document had pending and schedules what it should have
	 * now. Section 12: "changing valid_until cancels obsolete PENDING reminders
	 * and generates the new valid set."
	 *
	 * <p>Cancel-then-regenerate rather than a difference. Working out which of six
	 * offsets survived a date change is more code than throwing them away, and the
	 * rows are cancelled rather than deleted so the history still explains why
	 * nothing arrived.
	 */
	@Transactional
	public void reconcile(UUID accountId, VehicleDocument document) {
		cancelPendingFor(document.getId());

		NotificationPreferences preferences = preferencesService.preferencesOf(accountId);
		ZoneId zone = zoneOf(accountId);

		List<ReminderSchedule.ScheduledOffset> due = ReminderSchedule.futureFor(
				document.getValidUntil(), preferences, zone, clock.instant());

		for (ReminderSchedule.ScheduledOffset offset : due) {
			reminderRepository.save(
					new Reminder(document.getId(), offset.offsetDays(), offset.scheduledAt()));
		}
		reminderRepository.flush();

		log.info("Scheduled {} reminders for document {}", due.size(), document.getId());
	}

	/**
	 * Section 12's renewal clause: the superseded record's future reminders are
	 * cancelled and nothing takes their place. Not {@link #reconcile}, which would
	 * cancel them and schedule the identical set again - a renewal is the
	 * statement that this period has been replaced, so its warnings are no longer
	 * about anything.
	 */
	@Transactional
	public void cancelFor(UUID documentId) {
		int cancelled = cancelPendingFor(documentId);
		reminderRepository.flush();

		log.info("Cancelled {} pending reminders for superseded document {}", cancelled, documentId);
	}

	/**
	 * Section 12: "changing global reminder preferences reconciles future PENDING
	 * reminders only; SENT history is not rewritten." Every document in the
	 * garage, because a preference is not about one of them.
	 */
	@Transactional
	public void reconcileGarage(UUID accountId) {
		for (VehicleDocument document : documentRepository.ofGarage(accountId)) {
			reconcile(accountId, document);
		}
	}

	/**
	 * PENDING only. A reminder already SENT is history and section 12 forbids
	 * rewriting it; one already PROCESSING has been claimed by the scheduler and
	 * is not this transaction's to take back.
	 */
	private int cancelPendingFor(UUID documentId) {
		List<Reminder> pending = reminderRepository
				.findByVehicleDocumentIdAndStatus(documentId, ReminderStatus.PENDING);

		for (Reminder reminder : pending) {
			reminder.setStatus(ReminderStatus.CANCELLED);
		}
		reminderRepository.saveAll(pending);

		return pending.size();
	}

	/**
	 * The account's own zone. Loaded again here after the document service has
	 * already loaded it, which costs nothing: within one transaction Hibernate
	 * answers the second call from its first-level cache.
	 */
	private ZoneId zoneOf(UUID accountId) {
		User user = userRepository.findById(accountId)
				.orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

		return ZoneId.of(user.getTimezone());
	}
}