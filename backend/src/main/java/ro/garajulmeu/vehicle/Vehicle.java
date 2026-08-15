package ro.garajulmeu.vehicle;

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
 * A vehicle in somebody's garage. Specification section 10.2.
 *
 * <p>Deliberately thin. Section 9 makes the registration certificate the source
 * of truth for registration number, VIN, make and commercial description, and
 * forbids duplicating them here for convenience - so this entity knows who owns
 * the vehicle and what its owner chose to call it, and nothing else about what
 * the vehicle is.
 *
 * <p>The image columns exist in the table but are not mapped: they belong to
 * Phase 12, and Hibernate validates the mappings that exist rather than
 * demanding one per column.
 */
@Entity
@Table(name = "vehicles")
public class Vehicle {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	/** Optional nickname. When absent the client labels the vehicle from its certificate. */
	@Column(name = "display_name", length = 120)
	private String displayName;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Vehicle() {
		// Required by JPA.
	}

	public Vehicle(UUID userId) {
		this.userId = userId;
	}

	public UUID getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
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
		if (!(other instanceof Vehicle vehicle)) {
			return false;
		}
		return id != null && id.equals(vehicle.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}