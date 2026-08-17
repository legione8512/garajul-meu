package ro.garajulmeu.notification;

import java.time.Instant;
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
 * One message to one device. Specification section 10.8.
 *
 * <p>Both sides are held by id rather than by association, as everywhere else in
 * this project: the reminder lives in {@code reminder} and the device in
 * {@code push}, and nothing here needs to navigate to either.
 *
 * <p><strong>This row is written before the provider is called, not after.</strong>
 * A row created afterwards records only what succeeded, which makes a crash
 * during the attempt indistinguishable from an attempt that never happened - and
 * the unique constraint that protects the retry has nothing to match against.
 */
@Entity
@Table(name = "notification_deliveries")
public class NotificationDelivery {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "reminder_id", nullable = false)
	private UUID reminderId;

	@Column(name = "user_device_id", nullable = false)
	private UUID userDeviceId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 16)
	private DeliveryStatus status = DeliveryStatus.PENDING;

	@Column(name = "attempt_count", nullable = false)
	private int attemptCount;

	@Column(name = "sent_at")
	private Instant sentAt;

	/** A code, never a payload. Section 27. */
	@Column(name = "last_error_code", length = 64)
	private String lastErrorCode;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected NotificationDelivery() {
		// Required by JPA.
	}

	public NotificationDelivery(UUID reminderId, UUID userDeviceId) {
		this.reminderId = reminderId;
		this.userDeviceId = userDeviceId;
	}

	public UUID getId() {
		return id;
	}

	public UUID getReminderId() {
		return reminderId;
	}

	public UUID getUserDeviceId() {
		return userDeviceId;
	}

	public DeliveryStatus getStatus() {
		return status;
	}

	public void setStatus(DeliveryStatus status) {
		this.status = status;
	}

	public int getAttemptCount() {
		return attemptCount;
	}

	public void setAttemptCount(int attemptCount) {
		this.attemptCount = attemptCount;
	}

	public Instant getSentAt() {
		return sentAt;
	}

	public void setSentAt(Instant sentAt) {
		this.sentAt = sentAt;
	}

	public String getLastErrorCode() {
		return lastErrorCode;
	}

	public void setLastErrorCode(String lastErrorCode) {
		this.lastErrorCode = lastErrorCode;
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
		if (!(other instanceof NotificationDelivery delivery)) {
			return false;
		}
		return id != null && id.equals(delivery.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}