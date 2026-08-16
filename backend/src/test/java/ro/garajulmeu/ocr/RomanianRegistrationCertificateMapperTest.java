package ro.garajulmeu.ocr;

import java.util.List;

import org.junit.jupiter.api.Test;

import ro.garajulmeu.ocr.OcrDocument.Block;
import ro.garajulmeu.ocr.OcrDocument.Box;

import static org.assertj.core.api.Assertions.assertThat;

/** No Spring: the mapper takes its threshold from a record. */
class RomanianRegistrationCertificateMapperTest {

	private final RomanianRegistrationCertificateMapper mapper =
			new RomanianRegistrationCertificateMapper(
					new OcrProperties("stub", 10, 30, 1024, 200, 40_000_000, 0.80));

	private static Block at(String text, double confidence, double x, double y) {
		return new Block(text, confidence, new Box(x, y, 0.08, 0.03));
	}

	private OcrScan.ProposedField field(OcrScan scan, String name) {
		return scan.fields().stream()
				.filter(proposed -> proposed.field().equals(name))
				.findFirst().orElseThrow();
	}

	private OcrScan scanOf(Block... blocks) {
		return mapper.map(new OcrDocument(List.of(blocks)));
	}

	@Test
	void aCodeAndTheValueBesideItBecomeAProposal() {
		OcrScan scan = scanOf(at("A", 0.99, 0.02, 0.05), at("B 100 ABC", 0.95, 0.10, 0.05));

		assertThat(field(scan, "registrationNumber").value()).isEqualTo("B 100 ABC");
		assertThat(field(scan, "registrationNumber").status()).isEqualTo(FieldStatus.DETECTED);
	}

	/** Every field is answered for, so the screen never has to guess which state it is in. */
	@Test
	void everyCodedFieldAppearsExactlyOnce() {
		OcrScan scan = scanOf(at("A", 0.99, 0.02, 0.05));

		assertThat(scan.fields()).hasSize(CertificateCode.values().length);
		assertThat(scan.fields()).extracting(OcrScan.ProposedField::field).doesNotHaveDuplicates();
	}

	@Test
	void aCodeWithNothingBesideItIsNotDetected() {
		OcrScan scan = scanOf(at("A", 0.99, 0.02, 0.05));

		assertThat(field(scan, "registrationNumber").status()).isEqualTo(FieldStatus.NOT_DETECTED);
		assertThat(field(scan, "registrationNumber").value()).isNull();
	}

	/** Text on another line belongs to another field, however close it looks. */
	@Test
	void aValueOnADifferentLineIsNotTaken() {
		OcrScan scan = scanOf(at("A", 0.99, 0.02, 0.05), at("B 100 ABC", 0.95, 0.10, 0.40));

		assertThat(field(scan, "registrationNumber").status()).isEqualTo(FieldStatus.NOT_DETECTED);
	}

	@Test
	void anUncertainReadingIsProposedButFlagged() {
		OcrScan scan = scanOf(at("R", 0.99, 0.38, 0.23), at("albastru", 0.55, 0.46, 0.23));

		assertThat(field(scan, "colour").value()).isEqualTo("albastru");
		assertThat(field(scan, "colour").status()).isEqualTo(FieldStatus.NEEDS_REVIEW);
	}

	/**
	 * Section 13 asks for confidence and semantic validation together, and this is
	 * why: a VIN ending in I is a perfectly confident reading of something that
	 * cannot be a VIN, since I is excluded precisely because it looks like 1.
	 */
	@Test
	void aConfidentReadingThatCannotBeTrueStillNeedsReview() {
		OcrScan scan = scanOf(at("E", 0.99, 0.02, 0.24), at("VF1AAAAAAAA00000I", 0.99, 0.10, 0.24));

		assertThat(field(scan, "vin").status()).isEqualTo(FieldStatus.NEEDS_REVIEW);
		assertThat(field(scan, "vin").value()).isNull();
	}

	@Test
	void aRomanianDateIsProposedInTheFormTheFieldHolds() {
		OcrScan scan = scanOf(at("B", 0.99, 0.38, 0.05), at("14.03.2019", 0.96, 0.46, 0.05));

		assertThat(field(scan, "firstRegistrationDate").value()).isEqualTo("2019-03-14");
		assertThat(field(scan, "firstRegistrationDate").status()).isEqualTo(FieldStatus.DETECTED);
	}

	@Test
	void aDateNobodyCanParseProposesNothingAndAsksToBeChecked() {
		OcrScan scan = scanOf(at("B", 0.99, 0.38, 0.05), at("1?.0O.201", 0.93, 0.46, 0.05));

		assertThat(field(scan, "firstRegistrationDate").value()).isNull();
		assertThat(field(scan, "firstRegistrationDate").status()).isEqualTo(FieldStatus.NEEDS_REVIEW);
	}

	/** The unit is printed on the form; the reading is the number. */
	@Test
	void aNumberReadWithItsUnitIsStillANumber() {
		OcrScan scan = scanOf(
				at("F.1", 0.99, 0.38, 0.12), at("1780 kg", 0.97, 0.46, 0.12),
				at("P.2", 0.99, 0.53, 0.16), at("66,5 kW", 0.97, 0.61, 0.16));

		assertThat(field(scan, "maximumPermissibleMassKg").value()).isEqualTo("1780");
		assertThat(field(scan, "maximumPowerKw").value()).isEqualTo("66.5");
	}
}