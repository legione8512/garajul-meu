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
 * <p>The three image columns are mapped as of 12.3, after four phases of
 * existing in the table unmapped. They hold <strong>metadata and never
 * bytes</strong>, per section 22: the key names an object in the bucket, and the
 * type and size describe what was validated. A BLOB column would put megabytes
 * of photograph into every row a query touches.
 *
 * <p>The three move together or not at all, which is why they are written
 * through two methods rather than three setters. A key without a content type is
 * an image nothing can serve.
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

	/** An object key, never a URL and never a path on this machine. */
	@Column(name = "image_object_key", length = 255)
	private String imageObjectKey;

	/** Determined from the bytes by the validator, not from what the client declared. */
	@Column(name = "image_content_type", length = 100)
	private String imageContentType;

	@Column(name = "image_size_bytes")
	private Long imageSizeBytes;

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

	public String getImageObjectKey() {
		return imageObjectKey;
	}

	public String getImageContentType() {
		return imageContentType;
	}

	public Long getImageSizeBytes() {
		return imageSizeBytes;
	}

	public boolean hasImage() {
		return imageObjectKey != null;
	}

	/** All three at once: a key without a content type is an image nothing can serve. */
	public void setImage(String objectKey, String contentType, long sizeBytes) {
		this.imageObjectKey = objectKey;
		this.imageContentType = contentType;
		this.imageSizeBytes = sizeBytes;
	}

	public void clearImage() {
		this.imageObjectKey = null;
		this.imageContentType = null;
		this.imageSizeBytes = null;
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