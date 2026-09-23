package ro.garajulmeu.vehiclepayment.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Create and correct take the same body, as documents do: a correction replaces
 * the whole schedule, and its reminders are regenerated from it.
 *
 * @param kind             LOAN or CASCO
 * @param frequency        MONTHLY, QUARTERLY, SEMIANNUAL or ANNUAL
 * @param lastDueDate      where the series ends - give this or
 *                         {@code instalmentCount}, exactly one
 * @param instalmentCount  how many instalments there are, the first included
 * @param remindDaysBefore a subset of 7, 3, 1 and 0; absent or empty means the
 *                         owner's default, three days and one
 */
public record SavePaymentRequest(

		@NotBlank String kind,

		@NotNull LocalDate firstDueDate,

		@NotBlank String frequency,

		LocalDate lastDueDate,

		Integer instalmentCount,

		List<Integer> remindDaysBefore) {
}
