package ro.garajulmeu.vehicledocument.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A document as the client sends it, for both adding and correcting.
 *
 * <p><strong>A correction replaces the record rather than patching it</strong>,
 * as the registration certificate's does and for the same reason: screen 13
 * shows the whole document and sends it back whole, so an omitted optional field
 * means the user cleared it. Treating omission as "leave alone" would make
 * deleting a note impossible through the API.
 *
 * <p>{@code type} is text rather than the enum so that an unrecognised value can
 * be refused by name - see {@code DocumentType.of}. The lengths are the column
 * widths, so nothing is truncated silently on the way in; {@code notes} has none
 * because its column has none.
 */
public record SaveDocumentRequest(

		@NotBlank String type,

		/** Optional: section 10.4 allows an unknown start for an ITP or a rovinietă. */
		LocalDate validFrom,

		@NotNull LocalDate validUntil,

		@Size(max = 160) String provider,

		@Size(max = 64) String referenceNumber,

		String notes) {
}