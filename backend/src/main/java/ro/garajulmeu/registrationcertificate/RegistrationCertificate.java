package ro.garajulmeu.registrationcertificate;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * The source of truth for what a vehicle is. Specification sections 9 and 10.3.
 *
 * <p>Phase 7 maps exactly the four columns the schema declares NOT NULL -
 * registration number, make, commercial description and VIN. That is not an
 * arbitrary subset: it is every field a certificate cannot exist without, and
 * therefore the smallest thing that can identify a vehicle. Phase 8 adds the
 * thirty optional ones together with the field policy in section 8.
 *
 * <p>The owner is carried here as well as on the vehicle, so the database can
 * enforce one VIN per account. See the migration for why that cannot drift.
 */
@Entity
@Table(name = "registration_certificates")
public class RegistrationCertificate {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "vehicle_id", nullable = false)
	private UUID vehicleId;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "registration_number", nullable = false, length = 32)
	private String registrationNumber;

	@Column(nullable = false, length = 64)
	private String make;

	@Column(name = "commercial_description", nullable = false, length = 128)
	private String commercialDescription;

	@Column(nullable = false, length = 32)
	private String vin;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected RegistrationCertificate() {
		// Required by JPA.
	}

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

	public String getMake() {
		return make;
	}

	public String getCommercialDescription() {
		return commercialDescription;
	}

	public String getVin() {
		return vin;
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
}