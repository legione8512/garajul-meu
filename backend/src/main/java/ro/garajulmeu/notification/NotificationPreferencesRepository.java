package ro.garajulmeu.notification;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * One row per account or none at all, which is why the finder answers an
 * Optional rather than the caller assuming a row exists.
 */
public interface NotificationPreferencesRepository
		extends JpaRepository<NotificationPreferences, UUID> {

	Optional<NotificationPreferences> findByUserId(UUID userId);
}