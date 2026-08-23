package ro.garajulmeu.push;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import ro.garajulmeu.common.Sha256Hex;

/**
 * One installation of the native app. Specification section 10.7.
 *
 * <p><strong>This entity refuses to print itself.</strong> The registration
 * certificate does the same for the owner's address; here the reason is
 * sharper - a default {@code toString} reaching a log line would put a live push
 * token in it, and section 27 names full push tokens among the things that must
 * never be logged. The one place a fragment is written is
 * {@link LoggingPushNotificationProvider}, deliberately and partially.
 *
 * <p><strong>The token is encrypted in the column and plaintext in this
 * object.</strong> {@link PushTokenConverter} does the work on the way in and
 * out, so everything above this line - the dispatcher, the fingerprint, the
 * service - goes on seeing a usable token and nothing has to remember to
 * decrypt. That is the whole reason it is a converter and not a service call.
 *
 * <p>{@code pushTokenHash} exists because encryption forbids the two things the
 * column previously provided: an authenticated cipher produces different bytes
 * every time, so the row cannot be found by its value and a unique index on the
 * value can never collide. The hash is the blind index that restores both, and
 * it is derived here rather than by callers so it cannot drift from the token
 * it describes.
 */
@Entity
@Table(name = "user_devices")
public class UserDevice {

	/** Enough to recognise a device in a support conversation, useless to a thief. */
	private static final int FINGERPRINT_LENGTH = 6;

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "platform", nullable = false, length = 16)
	private DevicePlatform platform;

	@Convert(converter = PushTokenConverter.class)
	@Column(name = "push_token", nullable = false)
	private String pushToken;

	/** SHA-256 hex of the token. Deterministic, so it can be searched and made unique. */
	@Column(name = "push_token_hash", nullable = false, length = 64)
	private String pushTokenHash;

	@Column(name = "device_name", length = 120)
	private String deviceName;

	@Column(name = "notifications_enabled", nullable = false)
	private boolean notificationsEnabled = true;

	@Column(name = "token_updated_at", nullable = false)
	private Instant tokenUpdatedAt = Instant.now();

	@Column(name = "last_seen_at")
	private Instant lastSeenAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected UserDevice() {
		// Required by JPA.
	}

	public UserDevice(UUID userId, DevicePlatform platform, String pushToken) {
		this.userId = userId;
		this.platform = platform;
		// Assigned rather than routed through the setter: a constructor calling
		// an overridable method is a hazard, and tokenUpdatedAt already defaults
		// to now, which is what the setter would have set it to.
		this.pushToken = pushToken;
		this.pushTokenHash = Sha256Hex.of(pushToken);
	}

	/**
	 * The last few characters, for telling two devices apart in a log or a support
	 * conversation. Section 27 forbids logging <em>full</em> push tokens, and the
	 * word is doing work: a fragment identifies nothing on its own and cannot be
	 * used to send anything.
	 *
	 * <p>Reads the plaintext, which is what this object holds - the encryption
	 * lives in the column. A fingerprint of ciphertext would change every time the
	 * row was written and identify nothing at all.
	 */
	public String fingerprint() {
		if (pushToken == null || pushToken.length() <= FINGERPRINT_LENGTH) {
			return "…";
		}
		return "…" + pushToken.substring(pushToken.length() - FINGERPRINT_LENGTH);
	}

	public UUID getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public void setUserId(UUID userId) {
		this.userId = userId;
	}

	public DevicePlatform getPlatform() {
		return platform;
	}

	public void setPlatform(DevicePlatform platform) {
		this.platform = platform;
	}

	public String getPushToken() {
		return pushToken;
	}

	/** Keeps the blind index in step, because a stale one would lose the row. */
	public void setPushToken(String pushToken) {
		this.pushToken = pushToken;
		this.pushTokenHash = Sha256Hex.of(pushToken);
		this.tokenUpdatedAt = Instant.now();
	}

	public String getPushTokenHash() {
		return pushTokenHash;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	public boolean isNotificationsEnabled() {
		return notificationsEnabled;
	}

	public void setNotificationsEnabled(boolean notificationsEnabled) {
		this.notificationsEnabled = notificationsEnabled;
	}

	public Instant getTokenUpdatedAt() {
		return tokenUpdatedAt;
	}

	public Instant getLastSeenAt() {
		return lastSeenAt;
	}

	public void setLastSeenAt(Instant lastSeenAt) {
		this.lastSeenAt = lastSeenAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	/**
	 * Names the device and never its token. The default implementation would print
	 * every field, which is exactly the accident section 27 forbids.
	 */
	@Override
	public String toString() {
		return "UserDevice[id=" + id + ", platform=" + platform + ", token=" + fingerprint() + "]";
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof UserDevice device)) {
			return false;
		}
		return id != null && id.equals(device.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}