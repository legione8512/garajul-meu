package ro.garajulmeu.vehicledocument;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * One period of cover, or one inspection. Specification section 10.4.
 *
 * <p>A renewal is a new row rather than an edit, per section 12 - which is what
 * makes the history in 10.5 possible at all. Editing exists only for correcting
 * a record that was entered wrongly.
 *
 * <p>The vehicle is held by id rather than by association, as elsewhere in the
 * project: the two live in different feature packages and nothing here needs to
 * navigate to a Vehicle.
 */
@Entity
@Table(name = "vehicle_documents")
public class VehicleDocument {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "vehicle_id", nullable = false)
	private UUID vehicleId;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 16)
	private DocumentType type;

	/** Optional: section 10.4 allows it to be unknown for an ITP or a rovinietă. */
	@Column(name = "valid_from")
	private LocalDate validFrom;

	@Column(name = "valid_until", nullable = false)
	private LocalDate validUntil;

	/** The insurer, or the station that performed the inspection. */
	@Column(name = "provider", length = 160)
	private String provider;

	@Column(name = "reference_number", length = 64)
	private String referenceNumber;

	/** Free text, and the only unbounded column here. */
	@Column(name = "notes")
	private String notes;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected VehicleDocument() {
		// Required by JPA.
	}

	public VehicleDocument(UUID vehicleId, DocumentType type, LocalDate validUntil) {
		this.vehicleId = vehicleId;
		this.type = type;
		this.validUntil = validUntil;
	}

	public UUID getId() {
		return id;
	}

	public UUID getVehicleId() {
		return vehicleId;
	}

	public DocumentType getType() {
		return type;
	}

	public void setType(DocumentType type) {
		this.type = type;
	}

	public LocalDate getValidFrom() {
		return validFrom;
	}

	public void setValidFrom(LocalDate validFrom) {
		this.validFrom = validFrom;
	}

	public LocalDate getValidUntil() {
		return validUntil;
	}

	public void setValidUntil(LocalDate validUntil) {
		this.validUntil = validUntil;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public String getReferenceNumber() {
		return referenceNumber;
	}

	public void setReferenceNumber(String referenceNumber) {
		this.referenceNumber = referenceNumber;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
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
		if (!(other instanceof VehicleDocument document)) {
			return false;
		}
		return id != null && id.equals(document.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}