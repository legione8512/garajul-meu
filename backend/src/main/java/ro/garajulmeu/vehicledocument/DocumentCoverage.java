package ro.garajulmeu.vehicledocument;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * How long a document has left, and what that means. Specification section 11.
 *
 * <p><strong>The day belongs to the reader, not to the server.</strong> Section
 * 11 requires daysRemaining to be counted in the user's IANA timezone and not
 * against UTC date boundaries, which means the same row can be EXPIRES_TODAY for
 * somebody in Bucharest while it is still ACTIVE for somebody in Los Angeles.
 * {@link #todayFor} is the one place that decision is taken - the same shape as
 * {@code OcrQuota.allowanceDay}, and for the same reason: when the rule lived in
 * two places, they disagreed between midnight and three in the morning and seven
 * tests failed at 01:19 having been green for weeks.
 *
 * <p><strong>valid_until is inclusive.</strong> A document expiring today is not
 * expired today. That is one comparison and it is the one an off-by-one gets
 * wrong, which is why the bands are asserted at every boundary rather than in
 * the middle.
 */
public final class DocumentCoverage {

	private static final long URGENT_WITHIN = 7;
	private static final long EXPIRING_SOON_WITHIN = 30;

	private DocumentCoverage() {
	}

	/** The reader's today, in their own timezone. */
	public static LocalDate todayFor(Clock clock, ZoneId timezone) {
		return LocalDate.now(clock.withZone(timezone));
	}

	/**
	 * Whole local days from today to the last valid one. Zero on the day it
	 * expires, negative afterwards.
	 */
	public static long daysRemaining(LocalDate today, LocalDate validUntil) {
		return ChronoUnit.DAYS.between(today, validUntil);
	}

	public static DocumentStatus statusOn(LocalDate today, LocalDate validUntil) {
		long remaining = daysRemaining(today, validUntil);

		if (remaining < 0) {
			return DocumentStatus.EXPIRED;
		}
		if (remaining == 0) {
			return DocumentStatus.EXPIRES_TODAY;
		}
		if (remaining <= URGENT_WITHIN) {
			return DocumentStatus.URGENT;
		}
		if (remaining <= EXPIRING_SOON_WITHIN) {
			return DocumentStatus.EXPIRING_SOON;
		}
		return DocumentStatus.ACTIVE;
	}
}