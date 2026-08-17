package ro.garajulmeu.notification;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * What one account wants to be told about. Specification section 10.5.
 *
 * <p><strong>The defaults live here as field initialisers as well as in the
 * migration.</strong> That looks like duplication and is not: the column default
 * answers for a row the database inserts, and these answer for
 * {@link #defaultsFor}, which builds a set of preferences for an account that
 * has never saved any - a row that does not exist cannot have a column default.
 * The two must agree, and a test asserts that they do.
 */
@Entity
@Table(name = "notification_preferences")
public class NotificationPreferences {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "notifications_enabled", nullable = false)
	private boolean notificationsEnabled = true;

	@Column(name = "remind_30_days", nullable = false)
	private boolean remind30Days = true;

	@Column(name = "remind_14_days", nullable = false)
	private boolean remind14Days = true;

	@Column(name = "remind_7_days", nullable = false)
	private boolean remind7Days = true;

	@Column(name = "remind_3_days", nullable = false)
	private boolean remind3Days = true;

	@Column(name = "remind_1_day", nullable = false)
	private boolean remind1Day = true;

	@Column(name = "remind_on_expiry", nullable = false)
	private boolean remindOnExpiry = true;

	@Column(name = "notification_local_time", nullable = false)
	private LocalTime notificationLocalTime = LocalTime.of(9, 0);

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected NotificationPreferences() {
		// Required by JPA.
	}

	public NotificationPreferences(UUID userId) {
		this.userId = userId;
	}

	/**
	 * What an account that has never saved anything wants, without saving
	 * anything to say so. Returned by the read path and read by reminder
	 * generation, so both see the same answer for a person who has not been asked
	 * yet.
	 */
	public static NotificationPreferences defaultsFor(UUID userId) {
		return new NotificationPreferences(userId);
	}

	public UUID getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public boolean isNotificationsEnabled() {
		return notificationsEnabled;
	}

	public void setNotificationsEnabled(boolean notificationsEnabled) {
		this.notificationsEnabled = notificationsEnabled;
	}

	public boolean isRemind30Days() {
		return remind30Days;
	}

	public void setRemind30Days(boolean remind30Days) {
		this.remind30Days = remind30Days;
	}

	public boolean isRemind14Days() {
		return remind14Days;
	}

	public void setRemind14Days(boolean remind14Days) {
		this.remind14Days = remind14Days;
	}

	public boolean isRemind7Days() {
		return remind7Days;
	}

	public void setRemind7Days(boolean remind7Days) {
		this.remind7Days = remind7Days;
	}

	public boolean isRemind3Days() {
		return remind3Days;
	}

	public void setRemind3Days(boolean remind3Days) {
		this.remind3Days = remind3Days;
	}

	public boolean isRemind1Day() {
		return remind1Day;
	}

	public void setRemind1Day(boolean remind1Day) {
		this.remind1Day = remind1Day;
	}

	public boolean isRemindOnExpiry() {
		return remindOnExpiry;
	}

	public void setRemindOnExpiry(boolean remindOnExpiry) {
		this.remindOnExpiry = remindOnExpiry;
	}

	public LocalTime getNotificationLocalTime() {
		return notificationLocalTime;
	}

	public void setNotificationLocalTime(LocalTime notificationLocalTime) {
		this.notificationLocalTime = notificationLocalTime;
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
		if (!(other instanceof NotificationPreferences preferences)) {
			return false;
		}
		return id != null && id.equals(preferences.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}