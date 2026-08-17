package ro.garajulmeu.vehicledocument.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * The next period of cover. Section 12 makes a renewal a new row rather than an
 * edit, which is what makes the history in 10.5 possible at all.
 *
 * <p><strong>No type</strong>: the renewal takes it from the record it
 * supersedes, and that is precisely what distinguishes renewing from adding. A
 * different type is a different document.
 *
 * <p><strong>And nothing else is inherited.</strong> A new period of insurance
 * has a new policy number, so carrying the old one over would put two records in
 * the history claiming the same policy; and once the number is not inherited,
 * inheriting the insurer alone would be an arbitrary half-measure that the
 * screen can do better, since it already has the old record in front of it.
 *
 * <p>{@code validFrom} is not defaulted to the day after the superseded record
 * ends. That would be the application asserting continuous cover it has no way
 * of knowing about - people renew late, and a gap is a fact worth keeping rather
 * than papering over.
 */
public record RenewDocumentRequest(

		LocalDate validFrom,

		@NotNull LocalDate validUntil,

		@Size(max = 160) String provider,

		@Size(max = 64) String referenceNumber,

		String notes) implements DocumentPeriod {
}