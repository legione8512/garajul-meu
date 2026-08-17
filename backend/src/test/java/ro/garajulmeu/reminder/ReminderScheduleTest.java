package ro.garajulmeu.reminder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ro.garajulmeu.notification.NotificationPreferences;

import static org.assertj.core.api.Assertions.assertThat;

/** No Spring, no database: section 12's rules are arithmetic on dates and a zone. */
class ReminderScheduleTest {

	private static final ZoneId BUCHAREST = ZoneId.of("Europe/Bucharest");
	private static final LocalDate EXPIRES = LocalDate.of(2026, 12, 1);

	private static NotificationPreferences all() {
		return NotificationPreferences.defaultsFor(UUID.randomUUID());
	}

	/** Long before any of the offsets, so nothing is filtered for being past. */
	private static Instant longBefore() {
		return Instant.parse("2026-01-01T00:00:00Z");
	}

	@Test
	void theSixDefaultOffsetsAreScheduledLargestFirst() {
		assertThat(ReminderSchedule.futureFor(EXPIRES, all(), BUCHAREST, longBefore()))
				.extracting(ReminderSchedule.ScheduledOffset::offsetDays)
				.containsExactly(30, 14, 7, 3, 1, 0);
	}

	/** A switch turned off removes its offset and leaves the rest alone. */
	@Test
	void aSwitchTurnedOffRemovesOnlyItsOwnOffset() {
		NotificationPreferences preferences = all();
		preferences.setRemind7Days(false);
		preferences.setRemindOnExpiry(false);

		assertThat(ReminderSchedule.futureFor(EXPIRES, preferences, BUCHAREST, longBefore()))
				.extracting(ReminderSchedule.ScheduledOffset::offsetDays)
				.containsExactly(30, 14, 3, 1);
	}

	/**
	 * Nine in the morning where the person is, not where the server is. In
	 * December Bucharest is UTC+2, so the instant is 07:00 UTC.
	 */
	@Test
	void theInstantIsNineInTheAccountsOwnZone() {
		Instant at = ReminderSchedule.instantFor(EXPIRES, 30, LocalTime.of(9, 0), BUCHAREST);

		assertThat(at).isEqualTo(Instant.parse("2026-11-01T07:00:00Z"));
		assertThat(ReminderSchedule.instantFor(EXPIRES, 30, LocalTime.of(9, 0), ZoneId.of("UTC")))
				.isEqualTo(Instant.parse("2026-11-01T09:00:00Z"));
	}

	/**
	 * Section 12: past offsets are dropped, never collapsed into a burst of
	 * immediate messages. Somebody entering a policy that expires in two days gets
	 * the one-day and expiry-day reminders, not five at once for marks long gone.
	 */
	@Test
	void offsetsAlreadyPastAreDroppedRatherThanFiredAtOnce() {
		Instant now = ReminderSchedule.instantFor(EXPIRES, 2, LocalTime.of(12, 0), BUCHAREST);

		assertThat(ReminderSchedule.futureFor(EXPIRES, all(), BUCHAREST, now))
				.extracting(ReminderSchedule.ScheduledOffset::offsetDays)
				.containsExactly(1, 0);
	}

	/**
	 * The same comparison, seen from the other two rules section 12 states
	 * separately: a reminder falling today after the send time has gone is not
	 * scheduled, and an expired document schedules nothing at all.
	 */
	@Test
	void todayAfterTheSendTimeAndAnAlreadyExpiredDocumentBothScheduleNothing() {
		Instant justAfterNine = ReminderSchedule
				.instantFor(EXPIRES, 0, LocalTime.of(9, 1), BUCHAREST);

		assertThat(ReminderSchedule.futureFor(EXPIRES, all(), BUCHAREST, justAfterNine)).isEmpty();

		Instant wellAfter = ReminderSchedule.instantFor(EXPIRES.plusDays(30), 0,
				LocalTime.of(9, 0), BUCHAREST);

		assertThat(ReminderSchedule.futureFor(EXPIRES, all(), BUCHAREST, wellAfter)).isEmpty();
	}

	/** The same day, one minute before the hour, is still worth scheduling. */
	@Test
	void todayBeforeTheSendTimeIsStillScheduled() {
		Instant justBeforeNine = ReminderSchedule
				.instantFor(EXPIRES, 0, LocalTime.of(8, 59), BUCHAREST);

		assertThat(ReminderSchedule.futureFor(EXPIRES, all(), BUCHAREST, justBeforeNine))
				.extracting(ReminderSchedule.ScheduledOffset::offsetDays)
				.containsExactly(0);
	}
}