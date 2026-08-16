package ro.garajulmeu.registrationcertificate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * The source of truth for what a vehicle is. Specification sections 9 and 10.3,
 * with the field policy in section 8.
 *
 * <p>Four fields are required - registration number, make, commercial
 * description and VIN. Section 8 names exactly those as the minimum to save a
 * vehicle, and section 10.3 declares exactly those NOT NULL. The other
 * twenty-eight are optional and may stay empty forever; a certificate with only
 * the four is a legitimate, complete record as far as this application is
 * concerned.
 *
 * <p><strong>C.2 and C.3 are sensitive.</strong> Owner and legal-user names and
 * addresses are stored only to fulfil the digital-certificate function. Section
 * 8 forbids requiring them for reminders and forbids logging them; section 27
 * repeats the ban. Nothing in this package may put them in a log line, and that
 * includes their field names in a "changed fields" message.
 *
 * <p><strong>There is no ITP field, and there must never be one.</strong> The
 * certificate carries an X box for the next inspection date, but section 8 makes
 * it an ingestion source only: after confirmation it creates a VehicleDocument,
 * and that domain owns expiry and reminders. Storing X here would be a second
 * expiry value competing with the first.
 */
@Entity
@Table(name = "registration_certificates")
public class RegistrationCertificate {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "vehicle_id", nullable = false)
	private UUID vehicleId;

	/**
	 * Denormalised from the vehicle so the database can enforce one VIN per
	 * account. See the V4 migration for why it cannot drift.
	 */
	@Column(name = "user_id", nullable = false)
	private UUID userId;

	// ---- Identity. The four the schema declares NOT NULL. ----

	/** A */
	@Column(name = "registration_number", nullable = false, length = 32)
	private String registrationNumber;

	/** D.1 */
	@Column(nullable = false, length = 64)
	private String make;

	/** D.3 */
	@Column(name = "commercial_description", nullable = false, length = 128)
	private String commercialDescription;

	/** E */
	@Column(nullable = false, length = 32)
	private String vin;

	// ---- Dates and classification ----

	/** B */
	@Column(name = "first_registration_date")
	private LocalDate firstRegistrationDate;

	/** I */
	@Column(name = "registration_date")
	private LocalDate registrationDate;

	/** I.1 */
	@Column(name = "certificate_issue_date")
	private LocalDate certificateIssueDate;

	/** J */
	@Column(name = "vehicle_category", length = 16)
	private String vehicleCategory;

	/** D.2 */
	@Column(name = "type_variant_version", length = 128)
	private String typeVariantVersion;

	/** K */
	@Column(name = "type_approval_number", length = 64)
	private String typeApprovalNumber;

	/** H */
	@Column(name = "validity_period", length = 64)
	private String validityPeriod;

	// ---- Technical data ----

	/** F.1 */
	@Column(name = "maximum_permissible_mass_kg")
	private Integer maximumPermissibleMassKg;

	/** G */
	@Column(name = "vehicle_mass_kg")
	private Integer vehicleMassKg;

	/** P.1 */
	@Column(name = "engine_capacity_cc")
	private Integer engineCapacityCc;

	/** P.2 */
	@Column(name = "maximum_power_kw", precision = 8, scale = 2)
	private BigDecimal maximumPowerKw;

	/** P.3 */
	@Column(name = "fuel_type", length = 32)
	private String fuelType;

	/** Q */
	@Column(name = "power_weight_ratio", precision = 8, scale = 3)
	private BigDecimal powerWeightRatio;

	/** R */
	@Column(length = 64)
	private String colour;

	/** S.1 - seats including the driver */
	@Column
	private Integer seats;

	/** S.2 */
	@Column(name = "standing_places")
	private Integer standingPlaces;

	// ---- Administrative ----

	/** Y */
	@Column(name = "civ_number", length = 64)
	private String civNumber;

	/** Z */
	@Column(name = "issuing_authority", length = 128)
	private String issuingAuthority;

	/** No certificate code. Free text, and the only unbounded column here. */
	@Column
	private String observations;

	/** Printed twice on the certificate; stored once, per section 10.3. */
	@Column(name = "certificate_number", length = 64)
	private String certificateNumber;

	// ---- C.2, the owner. Sensitive: never required, never logged. ----

	/** C.2.1 */
	@Column(name = "owner_name_or_company", length = 160)
	private String ownerNameOrCompany;

	/** C.2.2 */
	@Column(name = "owner_first_name", length = 80)
	private String ownerFirstName;

	/** C.2.3 */
	@Column(name = "owner_address", length = 255)
	private String ownerAddress;

	/** The C2=C1 box on the certificate. */
	@Column(name = "c2_equals_c1")
	private Boolean c2EqualsC1;

	// ---- C.3, the legal user. Sensitive on the same terms. ----

	/** C.3.1 */
	@Column(name = "user_name_or_company", length = 160)
	private String userNameOrCompany;

	/** C.3.2 */
	@Column(name = "user_first_name", length = 80)
	private String userFirstName;

	/** C.3.3 */
	@Column(name = "user_address", length = 255)
	private String userAddress;

	/** The C3=C1 box on the certificate. */
	@Column(name = "c3_equals_c1")
	private Boolean c3EqualsC1;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected RegistrationCertificate() {
		// Required by JPA.
	}

	/**
	 * Only the four fields a certificate cannot exist without. Everything else is
	 * set afterwards, which is also how the correction endpoint works.
	 */
	public RegistrationCertificate(UUID vehicleId, UUID userId, String registrationNumber,
			String make, String commercialDescription, String vin) {
		this.vehicleId = vehicleId;
		this.userId = userId;
		this.registrationNumber = registrationNumber;
		this.make = make;
		this.commercialDescription = commercialDescription;
		this.vin = vin;
	}

	public UUID getId() {
		return id;
	}

	public UUID getVehicleId() {
		return vehicleId;
	}

	public UUID getUserId() {
		return userId;
	}

	public String getRegistrationNumber() {
		return registrationNumber;
	}

	public void setRegistrationNumber(String registrationNumber) {
		this.registrationNumber = registrationNumber;
	}

	public String getMake() {
		return make;
	}

	public void setMake(String make) {
		this.make = make;
	}

	public String getCommercialDescription() {
		return commercialDescription;
	}

	public void setCommercialDescription(String commercialDescription) {
		this.commercialDescription = commercialDescription;
	}

	public String getVin() {
		return vin;
	}

	public void setVin(String vin) {
		this.vin = vin;
	}

	public LocalDate getFirstRegistrationDate() {
		return firstRegistrationDate;
	}

	public void setFirstRegistrationDate(LocalDate firstRegistrationDate) {
		this.firstRegistrationDate = firstRegistrationDate;
	}

	public LocalDate getRegistrationDate() {
		return registrationDate;
	}

	public void setRegistrationDate(LocalDate registrationDate) {
		this.registrationDate = registrationDate;
	}

	public LocalDate getCertificateIssueDate() {
		return certificateIssueDate;
	}

	public void setCertificateIssueDate(LocalDate certificateIssueDate) {
		this.certificateIssueDate = certificateIssueDate;
	}

	public String getVehicleCategory() {
		return vehicleCategory;
	}

	public void setVehicleCategory(String vehicleCategory) {
		this.vehicleCategory = vehicleCategory;
	}

	public String getTypeVariantVersion() {
		return typeVariantVersion;
	}

	public void setTypeVariantVersion(String typeVariantVersion) {
		this.typeVariantVersion = typeVariantVersion;
	}

	public String getTypeApprovalNumber() {
		return typeApprovalNumber;
	}

	public void setTypeApprovalNumber(String typeApprovalNumber) {
		this.typeApprovalNumber = typeApprovalNumber;
	}

	public String getValidityPeriod() {
		return validityPeriod;
	}

	public void setValidityPeriod(String validityPeriod) {
		this.validityPeriod = validityPeriod;
	}

	public Integer getMaximumPermissibleMassKg() {
		return maximumPermissibleMassKg;
	}

	public void setMaximumPermissibleMassKg(Integer maximumPermissibleMassKg) {
		this.maximumPermissibleMassKg = maximumPermissibleMassKg;
	}

	public Integer getVehicleMassKg() {
		return vehicleMassKg;
	}

	public void setVehicleMassKg(Integer vehicleMassKg) {
		this.vehicleMassKg = vehicleMassKg;
	}

	public Integer getEngineCapacityCc() {
		return engineCapacityCc;
	}

	public void setEngineCapacityCc(Integer engineCapacityCc) {
		this.engineCapacityCc = engineCapacityCc;
	}

	public BigDecimal getMaximumPowerKw() {
		return maximumPowerKw;
	}

	public void setMaximumPowerKw(BigDecimal maximumPowerKw) {
		this.maximumPowerKw = maximumPowerKw;
	}

	public String getFuelType() {
		return fuelType;
	}

	public void setFuelType(String fuelType) {
		this.fuelType = fuelType;
	}

	public BigDecimal getPowerWeightRatio() {
		return powerWeightRatio;
	}

	public void setPowerWeightRatio(BigDecimal powerWeightRatio) {
		this.powerWeightRatio = powerWeightRatio;
	}

	public String getColour() {
		return colour;
	}

	public void setColour(String colour) {
		this.colour = colour;
	}

	public Integer getSeats() {
		return seats;
	}

	public void setSeats(Integer seats) {
		this.seats = seats;
	}

	public Integer getStandingPlaces() {
		return standingPlaces;
	}

	public void setStandingPlaces(Integer standingPlaces) {
		this.standingPlaces = standingPlaces;
	}

	public String getCivNumber() {
		return civNumber;
	}

	public void setCivNumber(String civNumber) {
		this.civNumber = civNumber;
	}

	public String getIssuingAuthority() {
		return issuingAuthority;
	}

	public void setIssuingAuthority(String issuingAuthority) {
		this.issuingAuthority = issuingAuthority;
	}

	public String getObservations() {
		return observations;
	}

	public void setObservations(String observations) {
		this.observations = observations;
	}

	public String getCertificateNumber() {
		return certificateNumber;
	}

	public void setCertificateNumber(String certificateNumber) {
		this.certificateNumber = certificateNumber;
	}

	public String getOwnerNameOrCompany() {
		return ownerNameOrCompany;
	}

	public void setOwnerNameOrCompany(String ownerNameOrCompany) {
		this.ownerNameOrCompany = ownerNameOrCompany;
	}

	public String getOwnerFirstName() {
		return ownerFirstName;
	}

	public void setOwnerFirstName(String ownerFirstName) {
		this.ownerFirstName = ownerFirstName;
	}

	public String getOwnerAddress() {
		return ownerAddress;
	}

	public void setOwnerAddress(String ownerAddress) {
		this.ownerAddress = ownerAddress;
	}

	public Boolean getC2EqualsC1() {
		return c2EqualsC1;
	}

	public void setC2EqualsC1(Boolean c2EqualsC1) {
		this.c2EqualsC1 = c2EqualsC1;
	}

	public String getUserNameOrCompany() {
		return userNameOrCompany;
	}

	public void setUserNameOrCompany(String userNameOrCompany) {
		this.userNameOrCompany = userNameOrCompany;
	}

	public String getUserFirstName() {
		return userFirstName;
	}

	public void setUserFirstName(String userFirstName) {
		this.userFirstName = userFirstName;
	}

	public String getUserAddress() {
		return userAddress;
	}

	public void setUserAddress(String userAddress) {
		this.userAddress = userAddress;
	}

	public Boolean getC3EqualsC1() {
		return c3EqualsC1;
	}

	public void setC3EqualsC1(Boolean c3EqualsC1) {
		this.c3EqualsC1 = c3EqualsC1;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof RegistrationCertificate certificate)) {
			return false;
		}
		return id != null && id.equals(certificate.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	/**
	 * Deliberately names nothing. The default would print every field, and this
	 * object holds the owner's name and home address - which section 8 forbids
	 * logging, and a toString reaches a log the moment anyone interpolates the
	 * entity into a message without thinking about it.
	 */
	@Override
	public String toString() {
		return "RegistrationCertificate[" + id + "]";
	}
}