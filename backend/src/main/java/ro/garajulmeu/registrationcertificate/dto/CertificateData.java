package ro.garajulmeu.registrationcertificate.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import ro.garajulmeu.registrationcertificate.CertificateValues;
import ro.garajulmeu.registrationcertificate.RegistrationCertificate;

/**
 * The whole certificate, in the order it is read on the document. Section 8.
 *
 * <p>One shape serves both directions, and that is the honest description of how
 * correction works here: <strong>the request replaces the optional block.</strong>
 * A field the body omits becomes empty rather than being left alone. Real merge
 * semantics would need to tell an absent field from an explicit null, which
 * Jackson cannot do for a plain record without extra machinery, and the only
 * client renders the whole certificate and saves it whole - so the distinction
 * never arises. Recorded as a deliberate narrowing of what PATCH usually means;
 * revisit if a second client wants to change one field.
 *
 * <p>The four required fields are the ones section 8 names as the minimum to
 * save a vehicle. Nothing else may be demanded - a certificate with only those
 * four is complete.
 *
 * <p>{@code @Digits} on the two decimals is not decoration. The columns are
 * DECIMAL(8,2) and DECIMAL(8,3); without these, a value with more digits reaches
 * PostgreSQL, fails there, and answers 500 for what is plainly a bad input.
 *
 * <p>Dates carry no constraint. Section 13 places semantic validation of dates
 * inside the OCR review, and a certificate is a transcription of a piece of
 * paper - if the paper says something odd, refusing to record it helps nobody.
 */
public record CertificateData(

		/** A */
		@NotBlank @Size(max = 32) String registrationNumber,

		/** B */
		LocalDate firstRegistrationDate,

		/** J */
		@Size(max = 16) String vehicleCategory,

		/** D.1 */
		@NotBlank @Size(max = 64) String make,

		/** D.2 */
		@Size(max = 128) String typeVariantVersion,

		/** D.3 */
		@NotBlank @Size(max = 128) String commercialDescription,

		/** E */
		@NotBlank @Size(max = 32) String vin,

		/** K */
		@Size(max = 64) String typeApprovalNumber,

		/** H */
		@Size(max = 64) String validityPeriod,

		/** I */
		LocalDate registrationDate,

		/** I.1 */
		LocalDate certificateIssueDate,

		/** F.1 */
		@PositiveOrZero Integer maximumPermissibleMassKg,

		/** G */
		@PositiveOrZero Integer vehicleMassKg,

		/** P.1 */
		@PositiveOrZero Integer engineCapacityCc,

		/** P.2 */
		@PositiveOrZero @Digits(integer = 6, fraction = 2) BigDecimal maximumPowerKw,

		/** P.3 */
		@Size(max = 32) String fuelType,

		/** Q */
		@PositiveOrZero @Digits(integer = 5, fraction = 3) BigDecimal powerWeightRatio,

		/** R */
		@Size(max = 64) String colour,

		/** S.1 - seats including the driver */
		@PositiveOrZero Integer seats,

		/** S.2 */
		@PositiveOrZero Integer standingPlaces,

		/** Y */
		@Size(max = 64) String civNumber,

		/** Z */
		@Size(max = 128) String issuingAuthority,

		/** Free text, and the only unbounded value here. */
		String observations,

		/** Printed twice on the certificate; stored once. */
		@Size(max = 64) String certificateNumber,

		/** C.2.1 - sensitive. Never required, never logged. */
		@Size(max = 160) String ownerNameOrCompany,

		/** C.2.2 - sensitive */
		@Size(max = 80) String ownerFirstName,

		/** C.2.3 - sensitive */
		@Size(max = 255) String ownerAddress,

		/** The C2=C1 box. Sensitive on the same terms. */
		Boolean c2EqualsC1,

		/** C.3.1 - sensitive */
		@Size(max = 160) String userNameOrCompany,

		/** C.3.2 - sensitive */
		@Size(max = 80) String userFirstName,

		/** C.3.3 - sensitive */
		@Size(max = 255) String userAddress,

		/** The C3=C1 box. Sensitive on the same terms. */
		Boolean c3EqualsC1) {

	public static CertificateData of(RegistrationCertificate certificate) {
		return new CertificateData(
				certificate.getRegistrationNumber(),
				certificate.getFirstRegistrationDate(),
				certificate.getVehicleCategory(),
				certificate.getMake(),
				certificate.getTypeVariantVersion(),
				certificate.getCommercialDescription(),
				certificate.getVin(),
				certificate.getTypeApprovalNumber(),
				certificate.getValidityPeriod(),
				certificate.getRegistrationDate(),
				certificate.getCertificateIssueDate(),
				certificate.getMaximumPermissibleMassKg(),
				certificate.getVehicleMassKg(),
				certificate.getEngineCapacityCc(),
				certificate.getMaximumPowerKw(),
				certificate.getFuelType(),
				certificate.getPowerWeightRatio(),
				certificate.getColour(),
				certificate.getSeats(),
				certificate.getStandingPlaces(),
				certificate.getCivNumber(),
				certificate.getIssuingAuthority(),
				certificate.getObservations(),
				certificate.getCertificateNumber(),
				certificate.getOwnerNameOrCompany(),
				certificate.getOwnerFirstName(),
				certificate.getOwnerAddress(),
				certificate.getC2EqualsC1(),
				certificate.getUserNameOrCompany(),
				certificate.getUserFirstName(),
				certificate.getUserAddress(),
				certificate.getC3EqualsC1());
	}

	/**
	 * Writes every field, including the ones this request left empty. That is the
	 * replacement semantics described above, in the one place it happens.
	 */
	public void applyTo(RegistrationCertificate certificate) {
		certificate.setRegistrationNumber(CertificateValues.normalisedRegistrationNumber(registrationNumber));
		certificate.setMake(make.trim());
		certificate.setCommercialDescription(commercialDescription.trim());
		certificate.setVin(CertificateValues.normalisedVin(vin));

		certificate.setFirstRegistrationDate(firstRegistrationDate);
		certificate.setRegistrationDate(registrationDate);
		certificate.setCertificateIssueDate(certificateIssueDate);
		certificate.setVehicleCategory(CertificateValues.trimmedOrNull(vehicleCategory));
		certificate.setTypeVariantVersion(CertificateValues.trimmedOrNull(typeVariantVersion));
		certificate.setTypeApprovalNumber(CertificateValues.trimmedOrNull(typeApprovalNumber));
		certificate.setValidityPeriod(CertificateValues.trimmedOrNull(validityPeriod));

		certificate.setMaximumPermissibleMassKg(maximumPermissibleMassKg);
		certificate.setVehicleMassKg(vehicleMassKg);
		certificate.setEngineCapacityCc(engineCapacityCc);
		certificate.setMaximumPowerKw(maximumPowerKw);
		certificate.setFuelType(CertificateValues.trimmedOrNull(fuelType));
		certificate.setPowerWeightRatio(powerWeightRatio);
		certificate.setColour(CertificateValues.trimmedOrNull(colour));
		certificate.setSeats(seats);
		certificate.setStandingPlaces(standingPlaces);

		certificate.setCivNumber(CertificateValues.trimmedOrNull(civNumber));
		certificate.setIssuingAuthority(CertificateValues.trimmedOrNull(issuingAuthority));
		certificate.setObservations(CertificateValues.trimmedOrNull(observations));
		certificate.setCertificateNumber(CertificateValues.trimmedOrNull(certificateNumber));

		certificate.setOwnerNameOrCompany(CertificateValues.trimmedOrNull(ownerNameOrCompany));
		certificate.setOwnerFirstName(CertificateValues.trimmedOrNull(ownerFirstName));
		certificate.setOwnerAddress(CertificateValues.trimmedOrNull(ownerAddress));
		certificate.setC2EqualsC1(c2EqualsC1);

		certificate.setUserNameOrCompany(CertificateValues.trimmedOrNull(userNameOrCompany));
		certificate.setUserFirstName(CertificateValues.trimmedOrNull(userFirstName));
		certificate.setUserAddress(CertificateValues.trimmedOrNull(userAddress));
		certificate.setC3EqualsC1(c3EqualsC1);
	}
}