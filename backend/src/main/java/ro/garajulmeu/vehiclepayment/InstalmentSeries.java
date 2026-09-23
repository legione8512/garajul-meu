package ro.garajulmeu.vehiclepayment;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The dates a recurring payment falls due on. Pure arithmetic, no Spring and no
 * database, so every rule below is tested in milliseconds.
 *
 * <p><strong>Every instalment is counted from the first one, never from the one
 * before it.</strong> {@code LocalDate.plusMonths} clamps to the end of a short
 * month - 31 January plus one month is 28 February - so stepping from the
 * previous date would carry the 28th into March and every month after it. From
 * the first date, the third instalment is 31 January plus two months, which is
 * 31 March again. That is the whole of the day-of-month rule in the design, and
 * it needs no code of its own beyond starting from the anchor.
 *
 * @param first     the first instalment, whose day of the month anchors the rest
 * @param frequency how many months apart they are
 * @param last      the last instalment's date; the series ends on or before it
 */
public record InstalmentSeries(LocalDate first, PaymentFrequency frequency, LocalDate last) {

	/**
	 * The ceiling on one payment: ten years of monthly instalments, which covers
	 * every Romanian car loan and leasing contract, and bounds how many reminder
	 * rows a single payment can create.
	 */
	public static final int MAX_INSTALMENTS = 120;

	/** The date of the instalment at this position, counting the first as 1. */
	public LocalDate dueDate(int instalment) {
		return first.plusMonths((long) (instalment - 1) * frequency.months());
	}

	/** How many instalments fall on or before {@link #last}. */
	public int count() {
		return countUpTo(first, frequency, last);
	}

	/** Every due date, first to last. At most {@link #MAX_INSTALMENTS} of them. */
	public List<LocalDate> dueDates() {
		int count = count();
		List<LocalDate> dates = new ArrayList<>(count);

		for (int instalment = 1; instalment <= count; instalment++) {
			dates.add(dueDate(instalment));
		}
		return List.copyOf(dates);
	}

	/**
	 * The first instalment falling on or after this day, if there is one left.
	 * Today's instalment is still "next" until the day is over, as a document is
	 * still valid on its last day.
	 */
	public Optional<Instalment> nextOnOrAfter(LocalDate day) {
		int count = count();

		for (int instalment = 1; instalment <= count; instalment++) {
			LocalDate due = dueDate(instalment);

			if (!due.isBefore(day)) {
				return Optional.of(new Instalment(instalment, count, due));
			}
		}
		return Optional.empty();
	}

	/** One instalment of the series: its position, the total, and its date. */
	public record Instalment(int number, int of, LocalDate dueDate) {
	}

	/**
	 * The last date of a series given as a number of instalments. The count is
	 * turned into a date once, at save, so only one rule decides where a series
	 * ends and a stored payment never has to remember which way it was entered.
	 */
	public static LocalDate lastDueDateFor(LocalDate first, PaymentFrequency frequency, int count) {
		return first.plusMonths((long) (count - 1) * frequency.months());
	}

	/**
	 * How many instalments fall between the first date and this last one.
	 * Counted by stepping from the anchor, for the same reason as
	 * {@link #dueDate}, and bounded one past the ceiling so that a last date in
	 * the next century answers "too many" instead of looping for a while.
	 */
	public static int countUpTo(LocalDate first, PaymentFrequency frequency, LocalDate last) {
		int count = 0;

		while (count <= MAX_INSTALMENTS
				&& !first.plusMonths((long) count * frequency.months()).isAfter(last)) {
			count++;
		}
		return count;
	}
}
