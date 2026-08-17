package ro.garajulmeu.reminder;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * No account id here, unlike every other repository in the project, and
 * deliberately: nothing reachable from the API reads this table yet. The view
 * section 16 describes arrives in 11.5 and will be scoped there, through the
 * document that owns the reminder.
 */
public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

	List<Reminder> findByVehicleDocumentIdAndStatus(UUID vehicleDocumentId, ReminderStatus status);

	List<Reminder> findByVehicleDocumentIdOrderByScheduledAt(UUID vehicleDocumentId);

	/**
	 * The scheduler's only question, answered with everything sending needs.
	 *
	 * <p><strong>No lower bound on {@code scheduledAt}</strong>, per section 12:
	 * "the scheduler queries overdue PENDING work so a short restart does not
	 * permanently lose a reminder". The word is "short", and nothing here enforces
	 * it - after a long outage an account receives several old reminders at once,
	 * which is the burst section 12 refuses at generation time. Recorded as a debt
	 * rather than fixed with a cutoff the specification does not ask for.
	 *
	 * <p>The certificate is an inner join because vehicle creation writes both
	 * rows in one transaction, so a vehicle without one cannot exist. If that ever
	 * changes, this join turns into a silent filter - reminders for the affected
	 * vehicles would simply never be found - so the atomicity in
	 * {@code VehicleService.create} is load-bearing here too.
	 *
	 * <p>The status is bound rather than written as a JPQL enum literal, matching
	 * every other query in the project.
	 */
	@Query("""
			select new ro.garajulmeu.reminder.DueReminder(
				r.id, r.offsetDays, v.userId, v.id, d.id, d.type, d.validUntil,
				coalesce(v.displayName, c.registrationNumber), u.preferredLanguage)
			from Reminder r
			join VehicleDocument d on d.id = r.vehicleDocumentId
			join Vehicle v on v.id = d.vehicleId
			join RegistrationCertificate c on c.vehicleId = v.id
			join User u on u.id = v.userId
			where r.status = :status and r.scheduledAt <= :now
			order by r.scheduledAt
			""")
	List<DueReminder> findDue(@Param("status") ReminderStatus status, @Param("now") Instant now,
			Limit limit);

	/**
	 * The claim, and the status in the name is what makes it one: a row that
	 * stopped being PENDING between the two queries is simply not returned, so it
	 * is never claimed twice.
	 */
	List<Reminder> findByIdInAndStatus(Collection<UUID> ids, ReminderStatus status);

	/** Reminders abandoned in PROCESSING, which only a crash can produce. */
	List<Reminder> findByStatusAndLastAttemptAtBefore(ReminderStatus status, Instant before);
}