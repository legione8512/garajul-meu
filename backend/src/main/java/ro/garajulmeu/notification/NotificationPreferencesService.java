package ro.garajulmeu.notification;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.garajulmeu.notification.dto.NotificationPreferencesView;
import ro.garajulmeu.notification.dto.SaveNotificationPreferencesRequest;

@Service
public class NotificationPreferencesService {

	private static final Logger log = LoggerFactory.getLogger(NotificationPreferencesService.class);

	private final NotificationPreferencesRepository repository;

	NotificationPreferencesService(NotificationPreferencesRepository repository) {
		this.repository = repository;
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
	 * The row for this account, existing or defaulted. Package-private because
	 * reminder generation in 11.2 needs the entity rather than the view, and must
	 * see exactly what the read endpoint reports.
	 */
	@Transactional(readOnly = true)
	NotificationPreferences preferencesOf(UUID accountId) {
		return repository.findByUserId(accountId)
				.orElseGet(() -> NotificationPreferences.defaultsFor(accountId));
	}

	/**
	 * Replaces the whole set, creating the row on first save.
	 *
	 * <p>Section 12 adds a consequence this does not have yet: changing these
	 * reconciles future PENDING reminders while leaving SENT history alone. There
	 * are no reminders until 11.2, and the hook belongs here when there are.
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