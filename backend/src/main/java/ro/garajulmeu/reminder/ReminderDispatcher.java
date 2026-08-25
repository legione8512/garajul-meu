package ro.garajulmeu.reminder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.notification.DeliveryStatus;
import ro.garajulmeu.notification.NotificationDelivery;
import ro.garajulmeu.notification.NotificationDeliveryRepository;
import ro.garajulmeu.push.PushNotification;
import ro.garajulmeu.push.PushNotificationProvider;
import ro.garajulmeu.push.PushTokenRejectedException;
import ro.garajulmeu.push.UserDevice;
import ro.garajulmeu.push.UserDeviceRepository;

/**
 * Turns due reminders into messages. Specification sections 12, 18 and 19.
 *
 * <p><strong>Three public methods, three transactions, and that is the
 * design.</strong> Claiming must be committed before the provider is called -
 * otherwise a crash during the network call rolls the claim back and the row
 * looks untouched, or the transaction stays open across an unbounded wait. The
 * orchestration therefore lives in {@link ReminderScheduler} and calls these in
 * order, rather than in a private method here: a {@code @Transactional} method
 * called from inside its own class never reaches the proxy, and the three
 * boundaries would silently collapse into none.
 *
 * <p><strong>The failure code written to a reminder is generic; the useful ones
 * are on the delivery rows.</strong> A reminder that reached one phone and not
 * another has no single error, and section 27 rules out putting anything richer
 * than a code in either place.
 */
@Service
public class ReminderDispatcher {

	private static final Logger log = LoggerFactory.getLogger(ReminderDispatcher.class);

	/** Written to the reminder when at least one device could not be reached. */
	private static final String DELIVERY_FAILED = "PUSH_DELIVERY_FAILED";

	/** What a provider that threw something other than an ApiException gets. */
	private static final String PROVIDER_ERROR = "PROVIDER_ERROR";

	/**
	 * The one failure that is about the device rather than the moment.
	 *
	 * <p>A string rather than an {@code ErrorCode}, for the reason given on
	 * {@link PushTokenRejectedException}: that enum's contract is that every
	 * failed API response carries one of its members and the frontend translates
	 * it, and this is never sent to anybody.
	 */
	private static final String TOKEN_REJECTED = "PUSH_TOKEN_REJECTED";

	private final ReminderRepository reminderRepository;
	private final NotificationDeliveryRepository deliveryRepository;
	private final UserDeviceRepository deviceRepository;
	private final PushNotificationProvider pushProvider;
	private final ReminderProperties properties;
	private final Clock clock;

	ReminderDispatcher(ReminderRepository reminderRepository,
			NotificationDeliveryRepository deliveryRepository,
			UserDeviceRepository deviceRepository, PushNotificationProvider pushProvider,
			ReminderProperties properties, Clock clock) {
		this.reminderRepository = reminderRepository;
		this.deliveryRepository = deliveryRepository;
		this.deviceRepository = deviceRepository;
		this.pushProvider = pushProvider;
		this.properties = properties;
		this.clock = clock;
	}

	/**
	 * Puts abandoned work back where the scheduler can see it.
	 *
	 * <p>Only a crash between claiming and finishing can leave a row in
	 * PROCESSING, so this normally releases nothing and logs nothing. It is not
	 * defensive programming: section 12 requires that a short restart lose no
	 * reminder, and PROCESSING is a state section 12's own query - PENDING and due
	 * - can never reach.
	 */
	@Transactional
	public int releaseStalled() {
		Instant before = clock.instant()
				.minus(Duration.ofMinutes(properties.staleAfterMinutes()));

		List<Reminder> stalled = reminderRepository
				.findByStatusAndLastAttemptAtBefore(ReminderStatus.PROCESSING, before);

		for (Reminder reminder : stalled) {
			reminder.setStatus(ReminderStatus.PENDING);
		}
		reminderRepository.saveAll(stalled);
		reminderRepository.flush();

		if (!stalled.isEmpty()) {
			log.warn("Released {} reminders abandoned in PROCESSING for over {} minutes",
					stalled.size(), properties.staleAfterMinutes());
		}
		return stalled.size();
	}

	/**
	 * Takes ownership of everything due, and answers with what it actually got.
	 *
	 * <p>The attempt is counted here rather than after sending, so a pass that
	 * dies mid-flight still spends one: a reminder whose delivery reliably crashes
	 * the process would otherwise be retried for ever.
	 */
	@Transactional
	public List<DueReminder> claimDue() {
		Instant now = clock.instant();

		List<DueReminder> due = reminderRepository.findDue(ReminderStatus.PENDING, now,
				Limit.of(properties.batchSize()));

		if (due.isEmpty()) {
			return List.of();
		}

		List<UUID> ids = due.stream().map(DueReminder::reminderId).toList();
		List<Reminder> claimed = reminderRepository.findByIdInAndStatus(ids, ReminderStatus.PENDING);

		for (Reminder reminder : claimed) {
			reminder.setStatus(ReminderStatus.PROCESSING);
			reminder.setLastAttemptAt(now);
			reminder.setAttemptCount(reminder.getAttemptCount() + 1);
		}
		reminderRepository.saveAll(claimed);
		reminderRepository.flush();

		Set<UUID> taken = claimed.stream().map(Reminder::getId).collect(Collectors.toSet());
		List<DueReminder> mine = due.stream()
				.filter(candidate -> taken.contains(candidate.reminderId()))
				.toList();

		log.info("Claimed {} of {} due reminders", mine.size(), due.size());
		return mine;
	}

	/**
	 * Sends one reminder to every device the account can be reached on.
	 *
	 * <p><strong>No device is not a failure.</strong> Section 18 makes push
	 * Android and iOS only and the applications are phases 17 and 18, so for the
	 * whole of V1 web this is every reminder. Calling it FAILED would retry every
	 * account without a phone three times a minute; calling it CANCELLED would
	 * claim it was superseded, which it was not. The reminder's status describes
	 * the work, and the work is finished - what actually went out is the delivery
	 * rows, and there are none.
	 *
	 * <p><strong>A device whose token the platform rejected is removed here, and
	 * the order of the two writes is load-bearing.</strong> V10 declares
	 * {@code ON DELETE CASCADE} from {@code notification_deliveries} to
	 * {@code user_devices}, so deleting a device takes its delivery rows with it -
	 * including the row just written to record why. The deliveries are therefore
	 * written first and read into {@code results} before anything is deleted; what
	 * survives the removal is this log line and the reminder's own code, which
	 * lives on a different table.
	 */
	@Transactional
	public void dispatch(DueReminder due) {
		Reminder reminder = reminderRepository.findById(due.reminderId()).orElseThrow();
		Instant now = clock.instant();

		List<UserDevice> devices = deviceRepository
				.findByUserIdAndNotificationsEnabledTrue(due.userId());

		if (devices.isEmpty()) {
			reminder.setStatus(ReminderStatus.SENT);
			reminder.setSentAt(now);
			reminderRepository.saveAndFlush(reminder);

			log.info("Reminder {} completed with nothing to deliver: account has no device",
					reminder.getId());
			return;
		}

		PushNotification notification = ReminderMessage.forSubject(due.subject(),
				due.offsetDays(), due.language());

		List<NotificationDelivery> results = new ArrayList<>();
		List<UserDevice> rejected = new ArrayList<>();

		for (UserDevice device : devices) {
			NotificationDelivery result = deliver(reminder, device, notification, now);
			results.add(result);

			if (TOKEN_REJECTED.equals(result.getLastErrorCode())) {
				rejected.add(device);
			}
		}

		long delivered = results.stream()
				.filter(delivery -> delivery.getStatus() == DeliveryStatus.SENT)
				.count();

		// Everything that failed for a reason another attempt could survive. A
		// rejected token is excluded on purpose: the handset is gone, and the row
		// is about to be gone too, so counting it as retryable would buy three
		// more passes against nothing.
		long retryable = results.size() - delivered - rejected.size();

		if (!rejected.isEmpty()) {
			deviceRepository.deleteAll(rejected);
			deviceRepository.flush();

			log.info("Removed {} device(s) the provider rejected permanently for account {}",
					rejected.size(), due.userId());
		}

		if (retryable > 0 && reminder.getAttemptCount() < properties.maxAttempts()) {
			// Back to PENDING with scheduled_at untouched, so it stays overdue and
			// the next pass takes it. Only the failed devices are attempted again -
			// the delivery rows remember which those are, and the rejected devices
			// are no longer in the query's answer at all.
			reminder.setStatus(ReminderStatus.PENDING);
			reminder.setLastErrorCode(DELIVERY_FAILED);
		} else if (delivered > 0) {
			// Somebody was told. A reminder that reached one of two phones did its
			// job, and the code stays on when it did not reach all of them.
			reminder.setStatus(ReminderStatus.SENT);
			reminder.setSentAt(now);
			reminder.setLastErrorCode(delivered == results.size() ? null : DELIVERY_FAILED);
		} else if (retryable > 0) {
			reminder.setStatus(ReminderStatus.FAILED);
			reminder.setLastErrorCode(DELIVERY_FAILED);
		} else {
			// Nothing was delivered and nothing is worth retrying, because every
			// device this account had has just been removed. That is the "no
			// device" case above arriving one pass late, and section 18's answer
			// has to be the same one: the work is finished, there was simply
			// nobody left to deliver to. The code stays on so that a reminder
			// which ended this way is distinguishable from one whose account
			// never had a phone.
			reminder.setStatus(ReminderStatus.SENT);
			reminder.setSentAt(now);
			reminder.setLastErrorCode(DELIVERY_FAILED);
		}
		reminderRepository.saveAndFlush(reminder);
	}

	/**
	 * One message to one device, recorded whatever happens.
	 *
	 * <p>The row is found or created <em>before</em> the provider is called, and
	 * the unique index on (reminder, device) is what makes finding it meaningful:
	 * a device that already has SENT against this reminder is skipped rather than
	 * sent to twice. That is section 10.8's "database idempotency guarantee" doing
	 * the only job it was ever given.
	 */
	private NotificationDelivery deliver(Reminder reminder, UserDevice device,
			PushNotification notification, Instant now) {
		NotificationDelivery delivery = deliveryRepository
				.findByReminderIdAndUserDeviceId(reminder.getId(), device.getId())
				.orElseGet(() -> new NotificationDelivery(reminder.getId(), device.getId()));

		if (delivery.getStatus() == DeliveryStatus.SENT) {
			return delivery;
		}

		delivery.setAttemptCount(delivery.getAttemptCount() + 1);

		try {
			pushProvider.send(device.getPushToken(), notification);

			delivery.setStatus(DeliveryStatus.SENT);
			delivery.setSentAt(now);
			delivery.setLastErrorCode(null);
		} catch (PushTokenRejectedException rejection) {
			// Caught before the general clause below, and that ordering is the
			// whole feature: this subclass would otherwise be swallowed as
			// PROVIDER_ERROR and retried like an outage. The caller reads this
			// code to decide the device is gone, so it must not be generic.
			delivery.setStatus(DeliveryStatus.FAILED);
			delivery.setLastErrorCode(TOKEN_REJECTED);

			log.info("Reminder {} found device {} permanently rejected; it will be removed",
					reminder.getId(), device.getId());
		} catch (RuntimeException exception) {
			// Broader than the ApiException the interface documents, on purpose: a
			// provider that throws something unexpected must cost one device, not
			// the whole batch.
			delivery.setStatus(DeliveryStatus.FAILED);
			delivery.setLastErrorCode(codeOf(exception));

			log.warn("Reminder {} was refused for device {} with {}", reminder.getId(),
					device.getId(), delivery.getLastErrorCode());
		}

		return deliveryRepository.saveAndFlush(delivery);
	}

	private static String codeOf(RuntimeException exception) {
		return exception instanceof ApiException apiException
				? apiException.errorCode().name()
				: PROVIDER_ERROR;
	}
}
