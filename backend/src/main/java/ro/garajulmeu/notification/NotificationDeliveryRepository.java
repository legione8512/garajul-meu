package ro.garajulmeu.notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * No account id, like {@code ReminderRepository} and for the same reason:
 * nothing reachable from the API reads this table. The delivery view in 11.5 is
 * scoped through the document that owns the reminder.
 */
public interface NotificationDeliveryRepository
		extends JpaRepository<NotificationDelivery, UUID> {

	/**
	 * The retry's first question, and the reason the unique index exists: has this
	 * reminder already been attempted against this device? Found means increment;
	 * absent means create.
	 */
	Optional<NotificationDelivery> findByReminderIdAndUserDeviceId(UUID reminderId,
			UUID userDeviceId);

	List<NotificationDelivery> findByReminderId(UUID reminderId);
}