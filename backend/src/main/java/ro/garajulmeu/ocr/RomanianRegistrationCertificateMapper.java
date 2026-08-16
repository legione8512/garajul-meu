package ro.garajulmeu.ocr;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Component;

import ro.garajulmeu.ocr.CertificateCode.Kind;
import ro.garajulmeu.ocr.OcrDocument.Block;

/**
 * Turns what a provider read into what the certificate screen can show.
 * Specification section 13 names this class and requires it to be
 * provider-independent, which it is: it sees only {@link OcrDocument}.
 *
 * <p><strong>It anchors on the printed codes, not on the template
 * coordinates.</strong> Phase 8 measured where each field sits on the approved
 * blank certificate, and it is tempting to reuse that here - but the provider
 * reports boxes on a photograph taken at some other angle, distance and
 * rotation. Matching the two would mean rectifying the image first, which
 * nothing asks for and which would add a source of error where none exists. The
 * printed code is on the same line as its value in every photograph of a
 * certificate, however it was taken.
 *
 * <p>The value taken is the nearest block to the right of the code on the same
 * line. That places a constraint on the adapter in 9.3: Document AI must be
 * asked for blocks at word or line granularity, because a provider returning one
 * block per paragraph would hand back the code and the value fused together.
 */
@Component
public class RomanianRegistrationCertificateMapper {

	/** How a Romanian certificate prints a date, and the separators a scan produces. */
	private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
			DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.ROOT),
			DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ROOT),
			DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT),
			DateTimeFormatter.ISO_LOCAL_DATE);

	/** Seventeen characters, and never I, O or Q - they would be read as 1 and 0. */
	private static final String VIN_PATTERN = "[A-HJ-NPR-Z0-9]{17}";

	private final OcrProperties properties;

	RomanianRegistrationCertificateMapper(OcrProperties properties) {
		this.properties = properties;
	}

	public OcrScan map(OcrDocument document) {
		List<OcrScan.ProposedField> proposals = new ArrayList<>();

		for (CertificateCode code : CertificateCode.values()) {
			proposals.add(propose(code, document));
		}

		return new OcrScan(List.copyOf(proposals));
	}

	private OcrScan.ProposedField propose(CertificateCode code, OcrDocument document) {
		Optional<Block> anchor = document.blocks().stream()
				.filter(block -> CertificateCode.ofPrinted(block.text())
						.filter(found -> found == code).isPresent())
				.findFirst();

		if (anchor.isEmpty()) {
			return nothing(code);
		}

		Optional<Block> value = valueBesideThe(anchor.get(), document);
		if (value.isEmpty()) {
			return nothing(code);
		}

		Block block = value.get();
		Optional<String> usable = read(code.kind(), block.text());

		if (usable.isEmpty()) {
			// Something was read and it does not hold up - a date nobody can parse,
			// a VIN of the wrong shape. The text is deliberately not proposed: the
			// field could not display it, and an empty box asking to be checked is
			// honest where a box full of nonsense is not.
			return new OcrScan.ProposedField(code.field(), null, block.confidence(), FieldStatus.NEEDS_REVIEW);
		}

		FieldStatus status = block.confidence() >= properties.confidenceThreshold()
				? FieldStatus.DETECTED
				: FieldStatus.NEEDS_REVIEW;

		return new OcrScan.ProposedField(code.field(), usable.get(), block.confidence(), status);
	}

	private static OcrScan.ProposedField nothing(CertificateCode code) {
		return new OcrScan.ProposedField(code.field(), null, 0, FieldStatus.NOT_DETECTED);
	}

	/**
	 * The nearest block to the right of the code, on the same line.
	 *
	 * <p>"The same line" is measured from the centres, with the code's own height
	 * as the tolerance - a photograph is never perfectly square to the camera, and
	 * demanding equal tops would lose the value on any picture taken by hand.
	 */
	private static Optional<Block> valueBesideThe(Block anchor, OcrDocument document) {
		double anchorCentre = anchor.box().y() + anchor.box().height() / 2;
		double anchorRight = anchor.box().x() + anchor.box().width();
		double tolerance = anchor.box().height();

		return document.blocks().stream()
				.filter(block -> block != anchor)
				.filter(block -> block.box().x() >= anchorRight - anchor.box().width() / 2)
				.filter(block -> Math.abs(block.box().y() + block.box().height() / 2 - anchorCentre) <= tolerance)
				.filter(block -> CertificateCode.ofPrinted(block.text()).isEmpty())
				.min(Comparator.comparingDouble(block -> block.box().x()));
	}

	/**
	 * Reads a value in the form its field can hold, or nothing at all.
	 *
	 * <p>This is where section 13's "semantic validation" lives. A VIN of sixteen
	 * characters and a date of the thirty-second of a month are both perfectly
	 * confident readings of something that cannot be true.
	 */
	private static Optional<String> read(Kind kind, String raw) {
		String text = raw.trim();

		if (text.isEmpty()) {
			return Optional.empty();
		}

		return switch (kind) {
			case TEXT -> Optional.of(text);
			case VIN -> vin(text);
			case DATE -> date(text);
			case INTEGER -> integer(text);
			case DECIMAL -> decimal(text);
		};
	}

	private static Optional<String> vin(String text) {
		String candidate = text.replaceAll("\\s", "").toUpperCase(Locale.ROOT);
		return candidate.matches(VIN_PATTERN) ? Optional.of(candidate) : Optional.empty();
	}

	/** Answers in ISO, because that is what a date field holds. */
	private static Optional<String> date(String text) {
		String candidate = text.replaceAll("\\s", "");

		for (DateTimeFormatter format : DATE_FORMATS) {
			try {
				return Optional.of(LocalDate.parse(candidate, format).toString());
			} catch (DateTimeParseException ignored) {
				// Try the next shape a certificate might have been printed in.
			}
		}
		return Optional.empty();
	}

	private static Optional<String> integer(String text) {
		// A scan of "1780 kg" is a reading of 1780; the unit is printed on the form.
		String digits = text.replaceAll("[^\\d]", "");
		return digits.isEmpty() ? Optional.empty() : Optional.of(digits);
	}

	private static Optional<String> decimal(String text) {
		String candidate = text.replaceAll("[^\\d,.]", "").replace(',', '.');

		try {
			return candidate.isEmpty() ? Optional.empty() : Optional.of(new BigDecimal(candidate).toPlainString());
		} catch (NumberFormatException exception) {
			return Optional.empty();
		}
	}
}