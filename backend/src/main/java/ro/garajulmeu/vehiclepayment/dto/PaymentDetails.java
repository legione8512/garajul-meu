package ro.garajulmeu.vehiclepayment.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import ro.garajulmeu.vehiclepayment.PaymentFrequency;
import ro.garajulmeu.vehiclepayment.PaymentKind;

/**
 * A payment as its owner reads it. The next instalment is worked out here, in
 * the account's own day, so the screen does no date arithmetic.
 *
 * @param instalmentCount  how many instalments the series has in all
 * @param nextDueDate      the next instalment still ahead, today included;
 *                         null once the last one has passed
 * @param nextInstalment   that instalment's position, counting the first as 1
 * @param daysUntilNext    days from today to it; 0 when it is due today
 */
public record PaymentDetails(
		UUID id,
		PaymentKind kind,
		LocalDate firstDueDate,
		PaymentFrequency frequency,
		LocalDate lastDueDate,
		int instalmentCount,
		List<Integer> remindDaysBefore,
		LocalDate nextDueDate,
		Integer nextInstalment,
		Long daysUntilNext) {
}
