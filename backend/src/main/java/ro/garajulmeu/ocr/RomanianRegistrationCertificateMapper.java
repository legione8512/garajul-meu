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
import ro.garajulmeu.ocr.OcrDocument.Box;

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
 * <p><strong>A value is several words, and the gaps say which ones.</strong>
 * Document AI reports one token per word, so "AUTOTURISM M1" arrives as two
 * blocks and taking the nearest one alone would propose half a category. The
 * words of one value are joined while the gap between them stays small. The
 * numbers are measured rather than chosen: on a photographed certificate put
 * through the real processor, words inside one value sat at most 0.006 apart, a
 * code sat 0.001 to 0.028 from the first word of its own value, and the nearest
 * thing belonging to another cell was 0.044 away. {@link #VALUE_GAP} sits in
 * that empty corridor.
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

	/**
	 * The widest gap that still belongs inside one cell, as a fraction of the page
	 * width. Above the largest measured code-to-value gap (0.028) and below the
	 * smallest measured gap to the next cell (0.044).
	 */
	private static final double VALUE_GAP = 0.035;

	/**
	 * The gap at which two words were printed apart rather than touching. A plate
	 * arrives as "CT" "-" "92" with gaps of 0.001 and an address as "Str" "."
	 * "ENACHITA" with 0.001 then 0.006; joining everything with a space would
	 * propose "CT - 92". The provider measured the space, so it is read rather
	 * than guessed.
	 */
	private static final double SPACE_GAP = 0.002;

	/** Down the page, then across it - so the same photograph always maps the same way. */
	private static final Comparator<Block> READING_ORDER =
			Comparator.<Block>comparingDouble(block -> block.box().y())
					.thenComparingDouble(block -> block.box().x());

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

	/**
	 * <p>A photographed certificate carries marks that read as codes but are not:
	 * a filled one produced a second "A" nowhere near the registration number.
	 * Anchors are therefore taken in reading order rather than in whatever order
	 * the provider listed them, and the first one that actually has a value beside
	 * it wins - a lone letter with empty page to its right cannot take a field
	 * away from the printed code.
	 */
	private OcrScan.ProposedField propose(CertificateCode code, OcrDocument document) {
		List<Block> anchors = document.blocks().stream()
				.filter(block -> CertificateCode.ofPrinted(block.text())
						.filter(found -> found == code).isPresent())
				.filter(block -> isNotSomebodyElsesValue(block, document))
				.sorted(READING_ORDER)
				.toList();

		for (Block anchor : anchors) {
			Optional<Block> value = valueRightOf(anchor, document);
			if (value.isPresent()) {
				return proposalFrom(code, value.get());
			}
		}

		return nothing(code);
	}

	private OcrScan.ProposedField proposalFrom(CertificateCode code, Block value) {
		Optional<String> usable = read(code.kind(), value.text());

		if (usable.isEmpty()) {
			// Something was read and it does not hold up - a date nobody can parse,
			// a VIN of the wrong shape. The text is deliberately not proposed: the
			// field could not display it, and an empty box asking to be checked is
			// honest where a box full of nonsense is not.
			return new OcrScan.ProposedField(code.field(), null, value.confidence(), FieldStatus.NEEDS_REVIEW);
		}

		FieldStatus status = value.confidence() >= properties.confidenceThreshold()
				? FieldStatus.DETECTED
				: FieldStatus.NEEDS_REVIEW;

		return new OcrScan.ProposedField(code.field(), usable.get(), value.confidence(), status);
	}

	private static OcrScan.ProposedField nothing(CertificateCode code) {
		return new OcrScan.ProposedField(code.field(), null, 0, FieldStatus.NOT_DETECTED);
	}

	/**
	 * A word that sits in another code's value box is that value, not a code.
	 *
	 * <p>Half the plates in the country begin with B, and B is also the code for
	 * the first registration date. Without this, a Bucharest certificate would
	 * offer "100 ABC" as a date. The test is the same corridor the join uses: no
	 * genuine code had another cell's content within {@link #VALUE_GAP} to its
	 * left on any of the twenty-seven codes measured.
	 */
	private static boolean isNotSomebodyElsesValue(Block block, OcrDocument document) {
		return document.blocks().stream()
				.filter(other -> other != block)
				.filter(other -> CertificateCode.ofPrinted(other.text()).isPresent())
				.noneMatch(other -> sitsInTheValueBoxOf(block, other));
	}

	/**
	 * "The same line" is measured from the centres, with the code's own height as
	 * the tolerance - a photograph is never perfectly square to the camera, and
	 * demanding equal tops would lose the value on any picture taken by hand. The
	 * lower bound on the gap is not zero because a value may be printed a hair
	 * inside its code's box: a power reading overlapped its P.2 by 0.004.
	 */
	private static boolean sitsInTheValueBoxOf(Block block, Block anchor) {
		if (Math.abs(centreOf(block) - centreOf(anchor)) > anchor.box().height()) {
			return false;
		}

		double gap = block.box().x() - rightOf(anchor);
		return gap <= VALUE_GAP && gap >= -anchor.box().width() / 2;
	}

	/** The words to the right of the code, up to the gap that means another cell. */
	private static Optional<Block> valueRightOf(Block anchor, OcrDocument document) {
		List<Block> line = document.blocks().stream()
				.filter(block -> block != anchor)
				.filter(block -> block.box().x() > anchor.box().x())
				.filter(block -> Math.abs(centreOf(block) - centreOf(anchor)) <= anchor.box().height())
				.sorted(Comparator.comparingDouble(block -> block.box().x()))
				.toList();

		List<Block> words = new ArrayList<>();
		double edge = rightOf(anchor);

		for (Block block : line) {
			double gap = block.box().x() - edge;

			if (words.isEmpty() && gap < -anchor.box().width() / 2) {
				continue;
			}
			if (gap > VALUE_GAP) {
				break;
			}
			// Not for the first word: a plate beginning with B is a value, not the
			// date code. From the second word on, a code means the next cell.
			if (!words.isEmpty() && CertificateCode.ofPrinted(block.text()).isPresent()) {
				break;
			}

			words.add(block);
			edge = Math.max(edge, rightOf(block));
		}

		return words.isEmpty() ? Optional.empty() : Optional.of(joined(words));
	}

	/**
	 * One block standing for the whole value. The confidence is the worst of the
	 * words, because a value is only as trustworthy as its least certain part -
	 * an address read perfectly except for the town is not a good address.
	 */
	private static Block joined(List<Block> words) {
		Block first = words.getFirst();
		StringBuilder text = new StringBuilder(first.text());
		double confidence = first.confidence();
		double left = first.box().x();
		double top = first.box().y();
		double right = rightOf(first);
		double bottom = bottomOf(first);

		for (Block word : words.subList(1, words.size())) {
			if (word.box().x() - right >= SPACE_GAP) {
				text.append(' ');
			}
			text.append(word.text());
			confidence = Math.min(confidence, word.confidence());
			top = Math.min(top, word.box().y());
			right = Math.max(right, rightOf(word));
			bottom = Math.max(bottom, bottomOf(word));
		}

		return new Block(text.toString(), confidence, new Box(left, top, right - left, bottom - top));
	}

	private static double centreOf(Block block) {
		return block.box().y() + block.box().height() / 2;
	}

	private static double rightOf(Block block) {
		return block.box().x() + block.box().width();
	}

	private static double bottomOf(Block block) {
		return block.box().y() + block.box().height();
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