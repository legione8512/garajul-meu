package ro.garajulmeu.dashboard.dto;

import java.time.LocalDate;
import java.util.UUID;

import ro.garajulmeu.vehiclepayment.PaymentKind;

/**
 * An instalment due soon, on its vehicle's card. 1.1, and only within
 * {@code DashboardService.PAYMENT_HORIZON_DAYS}: further off it stays on the
 * vehicle's own screen, so the card does not grow with every contract.
 *
 * @param daysRemaining 0 when it is due today
 * @param instalment    its position in the series, counting the first as 1
 * @param instalments   how many the series has in all
 */
public record PaymentDueLine(
		PaymentKind kind,
		UUID paymentId,
		LocalDate dueDate,
		long daysRemaining,
		int instalment,
		int instalments) {
}
