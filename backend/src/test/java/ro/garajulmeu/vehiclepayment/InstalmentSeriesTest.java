package ro.garajulmeu.vehiclepayment;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The dates of a recurring payment, 1.1. No Spring and no database: every rule
 * in docs/DESIGN_1_1_INSTALMENTS.md that is arithmetic is checked here.
 */
class InstalmentSeriesTest {

	private static InstalmentSeries monthly(LocalDate first, int count) {
		return new InstalmentSeries(first, PaymentFrequency.MONTHLY,
				InstalmentSeries.lastDueDateFor(first, PaymentFrequency.MONTHLY, count));
	}

	/**
	 * The design's own example. Stepping from the previous date would give 28
	 * March; counting from the anchor gives 31 March back.
	 */
	@Test
	void theThirtyFirstFallsOnTheLastDayOfShortMonthsAndComesBack() {
		InstalmentSeries series = monthly(LocalDate.of(2027, 1, 31), 4);

		assertThat(series.dueDates()).containsExactly(
				LocalDate.of(2027, 1, 31),
				LocalDate.of(2027, 2, 28),
				LocalDate.of(2027, 3, 31),
				LocalDate.of(2027, 4, 30));
	}

	@Test
	void aLeapYearKeepsTheTwentyNinth() {
		assertThat(monthly(LocalDate.of(2028, 1, 31), 2).dueDate(2))
				.isEqualTo(LocalDate.of(2028, 2, 29));
	}

	@Test
	void theFrequencyIsCountedInMonths() {
		LocalDate first = LocalDate.of(2026, 11, 15);

		assertThat(new InstalmentSeries(first, PaymentFrequency.QUARTERLY, LocalDate.of(2027, 8, 15))
				.dueDates()).containsExactly(first, LocalDate.of(2027, 2, 15),
						LocalDate.of(2027, 5, 15), LocalDate.of(2027, 8, 15));
		assertThat(new InstalmentSeries(first, PaymentFrequency.SEMIANNUAL, LocalDate.of(2027, 11, 15))
				.count()).isEqualTo(3);
		assertThat(new InstalmentSeries(first, PaymentFrequency.ANNUAL, LocalDate.of(2029, 11, 15))
				.count()).isEqualTo(4);
	}

	@Test
	void aCountBecomesTheLastInstalmentsDate() {
		assertThat(InstalmentSeries.lastDueDateFor(LocalDate.of(2026, 10, 15),
				PaymentFrequency.MONTHLY, 36)).isEqualTo(LocalDate.of(2029, 9, 15));
		assertThat(InstalmentSeries.lastDueDateFor(LocalDate.of(2026, 10, 15),
				PaymentFrequency.MONTHLY, 1)).isEqualTo(LocalDate.of(2026, 10, 15));
	}

	/** A contract's final date between two instalments ends the series at the one before it. */
	@Test
	void aLastDateBetweenTwoInstalmentsCountsOnlyThoseOnOrBeforeIt() {
		LocalDate first = LocalDate.of(2026, 10, 15);

		assertThat(InstalmentSeries.countUpTo(first, PaymentFrequency.MONTHLY, LocalDate.of(2026, 12, 20)))
				.isEqualTo(3);
		assertThat(InstalmentSeries.countUpTo(first, PaymentFrequency.MONTHLY, LocalDate.of(2026, 12, 15)))
				.isEqualTo(3);
		assertThat(InstalmentSeries.countUpTo(first, PaymentFrequency.MONTHLY, LocalDate.of(2026, 12, 14)))
				.isEqualTo(2);
	}

	@Test
	void anEndBeforeTheStartCountsNothing() {
		assertThat(InstalmentSeries.countUpTo(LocalDate.of(2026, 10, 15), PaymentFrequency.MONTHLY,
				LocalDate.of(2026, 10, 14))).isZero();
	}

	/** Bounded one past the ceiling, so a far-off date answers "too many" quickly. */
	@Test
	void countingStopsOnePastTheCeiling() {
		assertThat(InstalmentSeries.countUpTo(LocalDate.of(2026, 1, 1), PaymentFrequency.MONTHLY,
				LocalDate.of(2999, 1, 1))).isEqualTo(InstalmentSeries.MAX_INSTALMENTS + 1);
		assertThat(InstalmentSeries.countUpTo(LocalDate.of(2026, 1, 1), PaymentFrequency.MONTHLY,
				InstalmentSeries.lastDueDateFor(LocalDate.of(2026, 1, 1), PaymentFrequency.MONTHLY, 120)))
				.isEqualTo(120);
	}

	/** Today's instalment is still the next one until the day is over. */
	@Test
	void theNextInstalmentIncludesToday() {
		InstalmentSeries series = monthly(LocalDate.of(2026, 10, 15), 36);

		assertThat(series.nextOnOrAfter(LocalDate.of(2026, 11, 15)))
				.contains(new InstalmentSeries.Instalment(2, 36, LocalDate.of(2026, 11, 15)));
		assertThat(series.nextOnOrAfter(LocalDate.of(2026, 11, 16)))
				.contains(new InstalmentSeries.Instalment(3, 36, LocalDate.of(2026, 12, 15)));
		assertThat(series.nextOnOrAfter(LocalDate.of(2020, 1, 1)))
				.contains(new InstalmentSeries.Instalment(1, 36, LocalDate.of(2026, 10, 15)));
	}

	@Test
	void afterTheLastInstalmentThereIsNoNextOne() {
		assertThat(monthly(LocalDate.of(2026, 10, 15), 3).nextOnOrAfter(LocalDate.of(2026, 12, 16)))
				.isEmpty();
	}
}
