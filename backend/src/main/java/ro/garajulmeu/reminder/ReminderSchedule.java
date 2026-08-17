package ro.garajulmeu.reminder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import ro.garajulmeu.notification.NotificationPreferences;

/**
 * When an account should be told about a document. Specification section 12.
 *
 * <p><strong>Three of section 12's rules are one comparison.</strong> "Generate
 * only future reminder timestamps", "skip a reminder falling today whose local
 * send time has already passed", and "a document already expired creates no new
 * pending reminders" all reduce to: compute the instant, keep it if it is after
 * now. An expired document has every offset in the past; today-after-the-hour is
 * a past instant. Writing them as three checks would be three chances to
 * disagree with each other.
 *
 * <p><strong>The instant is computed from the account's zone, not the
 * server's.</strong> Nine in the morning is nine in the morning where the person
 * is - the third place this project takes that decision, after the OCR allowance
 * and the expiry bands, and the second where getting it wrong is invisible until
 * somebody is woken up.
 */
public final class ReminderSchedule {

	/** Section 12's default offsets, in the order they will fire. */
	static final int[] OFFSETS = { 30, 14, 7, 3, 1, 0 };

	private ReminderSchedule() {
	}

	/** One offset and the instant it falls on. */
	public record ScheduledOffset(int offsetDays, Instant scheduledAt) {
	}

	/**
	 * The offsets this account has switched on, largest first.
	 *
	 * <p>Zero is "on the day it expires", which is a reminder and not a formality:
	 * section 11 makes that day the last one still covered.
	 */
	public static List<Integer> offsetsFrom(NotificationPreferences preferences) {
		List<Integer> wanted = new ArrayList<>();

		if (preferences.isRemind30Days()) {
			wanted.add(30);
		}
		if (preferences.isRemind14Days()) {
			wanted.add(14);
		}
		if (preferences.isRemind7Days()) {
			wanted.add(7);
		}
		if (preferences.isRemind3Days()) {
			wanted.add(3);
		}
		if (preferences.isRemind1Day()) {
			wanted.add(1);
		}
		if (preferences.isRemindOnExpiry()) {
			wanted.add(0);
		}

		return List.copyOf(wanted);
	}

	/**
	 * The instant one offset falls on.
	 *
	 * <p>{@code ZonedDateTime.of} resolves a local time that does not exist by
	 * moving forward past the gap, which is what a clock changing forward in
	 * spring does to any alarm set inside the missing hour. Nine in the morning is
	 * never in that hour; a send time somebody configures later might be, and this
	 * answers the same way a phone would.
	 */
	public static Instant instantFor(LocalDate validUntil, int offsetDays,
			LocalTime localTime, ZoneId zone) {
		return ZonedDateTime.of(validUntil.minusDays(offsetDays), localTime, zone).toInstant();
	}

	/**
	 * Every reminder still worth scheduling for this document, and only those.
	 *
	 * <p>Past offsets are dropped rather than collapsed into one immediate
	 * message: section 12 is explicit that they "are not backfilled as multiple
	 * immediate push messages", and somebody entering a policy that expires
	 * tomorrow should get tomorrow's reminder, not five at once for the thirty,
	 * fourteen, seven and three-day marks that have already gone by.
	 */
	public static List<ScheduledOffset> futureFor(LocalDate validUntil,
			NotificationPreferences preferences, ZoneId zone, Instant now) {
		List<ScheduledOffset> scheduled = new ArrayList<>();

		for (int offset : offsetsFrom(preferences)) {
			Instant at = instantFor(validUntil, offset, preferences.getNotificationLocalTime(), zone);

			if (at.isAfter(now)) {
				scheduled.add(new ScheduledOffset(offset, at));
			}
		}

		return List.copyOf(scheduled);
	}
}