package ro.garajulmeu.vehicledocument.dto;

import java.time.LocalDate;
import java.util.UUID;

import ro.garajulmeu.vehicledocument.DocumentStatus;
import ro.garajulmeu.vehicledocument.DocumentType;

/**
 * One document, as the client sees it.
 *
 * <p>One shape for the list and for the detail, unlike vehicles. A vehicle's
 * summary leaves out the VIN because a list does not need it and section 24
 * argues for carrying identifying data only where it is used; a document has no
 * field the configure screen does not already show, so a second narrower record
 * would be duplication with nothing to justify it.
 *
 * <p><strong>{@code status} and {@code daysRemaining} are computed for this
 * request</strong>, in the reader's timezone, and are not columns. Section 11
 * defines both as statements about a document *and a date*.
 */
public record DocumentDetails(
		UUID id,
		DocumentType type,
		LocalDate validFrom,
		LocalDate validUntil,
		String provider,
		String referenceNumber,
		String notes,
		DocumentStatus status,
		long daysRemaining) {
}