package ro.garajulmeu.ocr;

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
 * One account's OCR requests on one day. Specification section 13.
 *
 * <p>A surrogate key with a unique index on (user_id, usage_date) rather than a
 * composite primary key, so this entity looks like every other one in the
 * project. The uniqueness that matters is enforced by the index either way.
 */
@Entity
@Table(name = "ocr_usage")
public class OcrUsage {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "usage_date", nullable = false)
	private LocalDate usageDate;

	@Column(name = "request_count", nullable = false)
	private int requestCount;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected OcrUsage() {
		// Required by JPA.
	}

	public OcrUsage(UUID userId, LocalDate usageDate) {
		this.userId = userId;
		this.usageDate = usageDate;
	}

	public UUID getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public LocalDate getUsageDate() {
		return usageDate;
	}

	public int getRequestCount() {
		return requestCount;
	}

	public void increment() {
		requestCount++;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof OcrUsage usage)) {
			return false;
		}
		return id != null && id.equals(usage.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}