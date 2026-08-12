package ro.garajulmeu.user;

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
 * An application account. Specification section 10.1.
 *
 * <p>The email is stored already normalised - trimmed and lower-cased - so the
 * unique index enforces one account per address regardless of how it was typed.
 * Normalisation is the service layer's job; this entity stores what it is given.
 */
@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "full_name", nullable = false, length = 120)
	private String fullName;

	@Column(nullable = false, length = 320)
	private String email;

	/** Argon2 output only. A plain password must never reach this field. */
	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	/** Null until the six-digit verification code has been accepted. */
	@Column(name = "email_verified_at")
	private Instant emailVerifiedAt;

	@Column(name = "preferred_language", nullable = false, length = 5)
	private Language preferredLanguage = Language.RO;

	/** IANA zone, used to compute local day boundaries for reminders. */
	@Column(nullable = false, length = 64)
	private String timezone = "Europe/Bucharest";

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected User() {
		// Required by JPA.
	}

	public User(String fullName, String email, String passwordHash) {
		this.fullName = fullName;
		this.email = email;
		this.passwordHash = passwordHash;
	}

	public UUID getId() {
		return id;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public Instant getEmailVerifiedAt() {
		return emailVerifiedAt;
	}

	public void setEmailVerifiedAt(Instant emailVerifiedAt) {
		this.emailVerifiedAt = emailVerifiedAt;
	}

	public boolean isEmailVerified() {
		return emailVerifiedAt != null;
	}

	public Language getPreferredLanguage() {
		return preferredLanguage;
	}

	public void setPreferredLanguage(Language preferredLanguage) {
		this.preferredLanguage = preferredLanguage;
	}

	public String getTimezone() {
		return timezone;
	}

	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	/**
	 * Identity is the database key alone. A transient User equals nothing but
	 * itself, which is what prevents two unsaved instances collapsing into one
	 * inside a Set.
	 */
	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof User user)) {
			return false;
		}
		return id != null && id.equals(user.id);
	}

	/**
	 * Constant by design. A hash derived from the id would change when Hibernate
	 * assigns it on flush, and an entity already placed in a HashSet would become
	 * unreachable in its own collection.
	 */
	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}
