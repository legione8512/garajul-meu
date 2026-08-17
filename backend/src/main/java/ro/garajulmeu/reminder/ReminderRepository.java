package ro.garajulmeu.reminder;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * No account id here, unlike every other repository in the project, and
 * deliberately: nothing reachable from the API reads this table yet. The view
 * section 16 describes arrives in 11.5 and will be scoped there, through the
 * document that owns the reminder.
 */
public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

	List<Reminder> findByVehicleDocumentIdAndStatus(UUID vehicleDocumentId, ReminderStatus status);

	List<Reminder> findByVehicleDocumentIdOrderByScheduledAt(UUID vehicleDocumentId);
}