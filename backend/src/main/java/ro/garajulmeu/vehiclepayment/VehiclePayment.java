package ro.garajulmeu.vehiclepayment;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
 * A recurring payment on a vehicle, added in 1.1: V13, and
 * docs/DESIGN_1_1_INSTALMENTS.md. Dates only - no amount - by the owner's
 * decision.
 */
@Entity
@Table(name = "vehicle_payments")
public class VehiclePayment {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "vehicle_id", nullable = false)
	private UUID vehicleId;

	@Enumerated(EnumType.STRING)
	@Column(name = "kind", nullable = false, length = 16)
	private PaymentKind kind;

	@Column(name = "first_due_date", nullable = false)
	private LocalDate firstDueDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "frequency", nullable = false, length = 16)
	private PaymentFrequency frequency;

	@Column(name = "last_due_date", nullable = false)
	private LocalDate lastDueDate;

	/** "3,1": a comma-separated subset of 7, 3, 1 and 0, largest first. */
	@Column(name = "remind_days_before", nullable = false, length = 16)
	private String remindDaysBefore;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected VehiclePayment() {
		// Required by JPA.
	}

	public VehiclePayment(UUID vehicleId) {
		this.vehicleId = vehicleId;
	}

	public UUID getId() {
		return id;
	}

	public UUID getVehicleId() {
		return vehicleId;
	}

	public PaymentKind getKind() {
		return kind;
	}

	public void setKind(PaymentKind kind) {
		this.kind = kind;
	}

	public LocalDate getFirstDueDate() {
		return firstDueDate;
	}

	public PaymentFrequency getFrequency() {
		return frequency;
	}

	public LocalDate getLastDueDate() {
		return lastDueDate;
	}

	/** The three together, because none of them means anything without the others. */
	public void setSchedule(LocalDate firstDueDate, PaymentFrequency frequency, LocalDate lastDueDate) {
		this.firstDueDate = firstDueDate;
		this.frequency = frequency;
		this.lastDueDate = lastDueDate;
	}

	public InstalmentSeries series() {
		return new InstalmentSeries(firstDueDate, frequency, lastDueDate);
	}

	public List<Integer> getRemindDaysBefore() {
		if (remindDaysBefore == null || remindDaysBefore.isBlank()) {
			return List.of();
		}
		return Arrays.stream(remindDaysBefore.split(","))
				.map(String::trim)
				.map(Integer::valueOf)
				.toList();
	}

	public void setRemindDaysBefore(List<Integer> days) {
		this.remindDaysBefore = days.stream()
				.map(String::valueOf)
				.collect(Collectors.joining(","));
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
		if (!(other instanceof VehiclePayment payment)) {
			return false;
		}
		return id != null && id.equals(payment.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}
