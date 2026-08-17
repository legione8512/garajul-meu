package ro.garajulmeu.reminder;

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

/** One scheduled nudge. Specification section 10.6. */
@Entity
@Table(name = "reminders")
public class Reminder {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "vehicle_document_id", nullable = false)
	private UUID vehicleDocumentId;

	@Column(name = "offset_days", nullable = false)
	private int offsetDays;

	@Column(name = "scheduled_at", nullable = false)
	private Instant scheduledAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 16)
	private ReminderStatus status = ReminderStatus.PENDING;

	@Column(name = "attempt_count", nullable = false)
	private int attemptCount;

	@Column(name = "last_attempt_at")
	private Instant lastAttemptAt;

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

	protected Reminder() {
		// Required by JPA.
	}

	public Reminder(UUID vehicleDocumentId, int offsetDays, Instant scheduledAt) {
		this.vehicleDocumentId = vehicleDocumentId;
		this.offsetDays = offsetDays;
		this.scheduledAt = scheduledAt;
	}

	public UUID getId() {
		return id;
	}

	public UUID getVehicleDocumentId() {
		return vehicleDocumentId;
	}

	public int getOffsetDays() {
		return offsetDays;
	}

	public Instant getScheduledAt() {
		return scheduledAt;
	}

	public ReminderStatus getStatus() {
		return status;
	}

	public void setStatus(ReminderStatus status) {
		this.status = status;
	}

	public int getAttemptCount() {
		return attemptCount;
	}

	public void setAttemptCount(int attemptCount) {
		this.attemptCount = attemptCount;
	}

	public Instant getLastAttemptAt() {
		return lastAttemptAt;
	}

	public void setLastAttemptAt(Instant lastAttemptAt) {
		this.lastAttemptAt = lastAttemptAt;
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
		if (!(other instanceof Reminder reminder)) {
			return false;
		}
		return id != null && id.equals(reminder.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}