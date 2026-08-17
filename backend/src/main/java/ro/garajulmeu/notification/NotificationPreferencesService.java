package ro.garajulmeu.notification;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.garajulmeu.notification.dto.NotificationPreferencesView;
import ro.garajulmeu.notification.dto.SaveNotificationPreferencesRequest;
import ro.garajulmeu.reminder.ReminderService;

@Service
public class NotificationPreferencesService {

	private static final Logger log = LoggerFactory.getLogger(NotificationPreferencesService.class);

	private final NotificationPreferencesRepository repository;

	/**
	 * Reached through the context rather than injected, and only here.
	 * ReminderService needs these preferences to decide what to schedule, and this
	 * needs ReminderService to reconcile after a change - a constructor cycle
	 * Spring cannot resolve. The lazy lookup breaks it at the one point where the
	 * two genuinely call each other, without pushing an event bus through the
	 * whole design to avoid saying so.
	 */
	private final ApplicationContext context;

	NotificationPreferencesService(NotificationPreferencesRepository repository,
			ApplicationContext context) {
		this.repository = repository;
		this.context = context;
	}

	/**
	 * What this account wants, whether or not it has ever said so.
	 *
	 * <p><strong>A missing row answers with the defaults and writes nothing.</strong>
	 * Creating one here would make every read a write - on a screen somebody may
	 * only be looking at - and would leave the same values in the database that
	 * the absence already means.
	 */
	@Transactional(readOnly = true)
	public NotificationPreferencesView of(UUID accountId) {
		return view(preferencesOf(accountId));
	}

	/**
	 * The row for this account, existing or defaulted. Public because reminder
	 * generation reads it, and must see exactly what the read endpoint reports.
	 */
	@Transactional(readOnly = true)
	public NotificationPreferences preferencesOf(UUID accountId) {
		return repository.findByUserId(accountId)
				.orElseGet(() -> NotificationPreferences.defaultsFor(accountId));
	}

	/**
	 * Replaces the whole set, creating the row on first save.
	 *
	 * <p>Section 12: "changing global reminder preferences reconciles future
	 * PENDING reminders only; SENT history is not rewritten." Reconciliation runs
	 * over every document in the garage, because a preference is not about one of
	 * them.
	 */
	@Transactional
	public NotificationPreferencesView replace(UUID accountId,
			SaveNotificationPreferencesRequest request) {
		NotificationPreferences preferences = repository.findByUserId(accountId)
				.orElseGet(() -> new NotificationPreferences(accountId));

		preferences.setNotificationsEnabled(request.notificationsEnabled());
		preferences.setRemind30Days(request.remind30Days());
		preferences.setRemind14Days(request.remind14Days());
		preferences.setRemind7Days(request.remind7Days());
		preferences.setRemind3Days(request.remind3Days());
		preferences.setRemind1Day(request.remind1Day());
		preferences.setRemindOnExpiry(request.remindOnExpiry());
		preferences.setNotificationLocalTime(request.notificationLocalTime());

		repository.saveAndFlush(preferences);
		context.getBean(ReminderService.class).reconcileGarage(accountId);

		log.info("Updated notification preferences for account {}", accountId);

		return view(preferences);
	}

	private static NotificationPreferencesView view(NotificationPreferences preferences) {
		return new NotificationPreferencesView(
				preferences.isNotificationsEnabled(),
				preferences.isRemind30Days(),
				preferences.isRemind14Days(),
				preferences.isRemind7Days(),
				preferences.isRemind3Days(),
				preferences.isRemind1Day(),
				preferences.isRemindOnExpiry(),
				preferences.getNotificationLocalTime());
	}
}