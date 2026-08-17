package ro.garajulmeu.vehicledocument;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * How long a document has left, what that means, and which of several records is
 * the one that counts. Specification section 11.
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
 *
 * <p><strong>The selection rule lives here and nowhere else.</strong> It was
 * briefly a JPQL query as well; two implementations of one rule is the drift
 * this class exists to prevent, and the dashboard needs the whole garage in one
 * query anyway - so the choosing is done over records already in hand.
 */
public final class DocumentCoverage {

	private static final long URGENT_WITHIN = 7;
	private static final long EXPIRING_SOON_WITHIN = 30;

	/**
	 * Section 11: greatest {@code valid_from} wins, ties resolve by the newest
	 * row. A missing start means "already effective", so it loses to any explicit
	 * one - the same order {@code desc nulls last} gives in SQL.
	 *
	 * <p>Both keys tolerate null. {@code valid_from} because the column does, and
	 * {@code created_at} because an entity that has not been persisted has no
	 * timestamp yet and a comparator must not explode on one.
	 */
	private static final Comparator<VehicleDocument> MOST_RECENTLY_STARTED =
			Comparator.comparing(VehicleDocument::getValidFrom,
							Comparator.nullsFirst(Comparator.naturalOrder()))
					.thenComparing(VehicleDocument::getCreatedAt,
							Comparator.nullsFirst(Comparator.naturalOrder()));

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

	/** The record covering today, of those given, or none. */
	public static Optional<VehicleDocument> coveringOn(List<VehicleDocument> records, LocalDate today) {
		return records.stream()
				.filter(record -> covers(record, today))
				.max(MOST_RECENTLY_STARTED);
	}

	/**
	 * The soonest record that has not started yet. Section 11 allows this to be
	 * shown separately when nothing covers today - and forbids showing it as
	 * though it were active, which is the whole reason it is a second question
	 * rather than a fallback for the first.
	 */
	public static Optional<VehicleDocument> upcomingAfter(List<VehicleDocument> records, LocalDate today) {
		return records.stream()
				.filter(record -> record.getValidFrom() != null
						&& record.getValidFrom().isAfter(today))
				.min(Comparator.comparing(VehicleDocument::getValidFrom));
	}

	/** The most recently ended record, for saying when cover actually lapsed. */
	public static Optional<VehicleDocument> lastToExpire(List<VehicleDocument> records, LocalDate today) {
		return records.stream()
				.filter(record -> record.getValidUntil().isBefore(today))
				.max(Comparator.comparing(VehicleDocument::getValidUntil));
	}

	private static boolean covers(VehicleDocument record, LocalDate today) {
		return (record.getValidFrom() == null || !record.getValidFrom().isAfter(today))
				&& !record.getValidUntil().isBefore(today);
	}
}