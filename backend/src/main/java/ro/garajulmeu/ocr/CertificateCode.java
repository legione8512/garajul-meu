package ro.garajulmeu.ocr;

import java.util.Optional;

/**
 * The printed codes on a Romanian registration certificate, and the field each
 * one labels. Specification section 8 calls these mapping metadata rather than
 * names, which is exactly what they are used for here.
 *
 * <p>The codes are written down on both sides of the application - here and in
 * the frontend's field table - and there is nothing linking the two. That is
 * accepted duplication, of the same kind as the validation bounds: both are read
 * from section 8, and joining them would mean shipping the specification as
 * data. It is recorded rather than hidden.
 *
 * <p>Four fields have no code and are therefore never proposed by OCR:
 * observations and the certificate number are printed without one, and the two
 * C2=C1 / C3=C1 boxes are ticks rather than text.
 */
public enum CertificateCode {

	A("A", "registrationNumber", Kind.TEXT),
	B("B", "firstRegistrationDate", Kind.DATE),
	J("J", "vehicleCategory", Kind.TEXT),
	D1("D.1", "make", Kind.TEXT),
	D2("D.2", "typeVariantVersion", Kind.TEXT),
	D3("D.3", "commercialDescription", Kind.TEXT),
	E("E", "vin", Kind.VIN),
	K("K", "typeApprovalNumber", Kind.TEXT),
	H("H", "validityPeriod", Kind.TEXT),
	I("I", "registrationDate", Kind.DATE),
	I1("I.1", "certificateIssueDate", Kind.DATE),
	F1("F.1", "maximumPermissibleMassKg", Kind.INTEGER),
	G("G", "vehicleMassKg", Kind.INTEGER),
	P1("P.1", "engineCapacityCc", Kind.INTEGER),
	P2("P.2", "maximumPowerKw", Kind.DECIMAL),
	P3("P.3", "fuelType", Kind.TEXT),
	Q("Q", "powerWeightRatio", Kind.DECIMAL),
	R("R", "colour", Kind.TEXT),
	S1("S.1", "seats", Kind.INTEGER),
	S2("S.2", "standingPlaces", Kind.INTEGER),
	Y("Y", "civNumber", Kind.TEXT),
	Z("Z", "issuingAuthority", Kind.TEXT),
	C21("C.2.1", "ownerNameOrCompany", Kind.TEXT),
	C22("C.2.2", "ownerFirstName", Kind.TEXT),
	C23("C.2.3", "ownerAddress", Kind.TEXT),
	C31("C.3.1", "userNameOrCompany", Kind.TEXT),
	C32("C.3.2", "userFirstName", Kind.TEXT),
	C33("C.3.3", "userAddress", Kind.TEXT);

	/** How a value has to be read, which is also how it can be found wrong. */
	public enum Kind {
		TEXT, DATE, INTEGER, DECIMAL, VIN
	}

	private final String printed;
	private final String field;
	private final Kind kind;

	CertificateCode(String printed, String field, Kind kind) {
		this.printed = printed;
		this.field = field;
		this.kind = kind;
	}

	public String printed() {
		return printed;
	}

	public String field() {
		return field;
	}

	public Kind kind() {
		return kind;
	}

	/**
	 * Matches the text of a block against the printed codes. Trailing separators
	 * are ignored because a scan of "D.1" beside a box often arrives as "D.1:" or
	 * "D.1)", and refusing those would lose the anchor over punctuation.
	 */
	public static Optional<CertificateCode> ofPrinted(String text) {
		String cleaned = text.trim().replaceAll("[:)\\]]+$", "");

		for (CertificateCode code : values()) {
			if (code.printed.equalsIgnoreCase(cleaned)) {
				return Optional.of(code);
			}
		}
		return Optional.empty();
	}
}