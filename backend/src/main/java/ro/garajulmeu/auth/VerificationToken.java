package ro.garajulmeu.auth;

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

/**
 * A single-use six-digit code. Specification section 10.10.
 *
 * <p>The owner is held as a plain {@code userId} rather than a {@code @ManyToOne}
 * association. The auth module never needs the User object from a token - it
 * already has it - and the plain key keeps this module independent of the user
 * entity. Referential integrity is still enforced, by the foreign key in the
 * migration.
 *
 * <p>Only the hash of the code is stored. Reading this table reveals nothing
 * that could verify an account.
 */
@Entity
@Table(name = "verification_tokens")
public class VerificationToken {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private VerificationTokenType type;

	@Column(name = "token_hash", nullable = false)
	private String tokenHash;

	/** The requested new address, for EMAIL_CHANGE only. Null otherwise. */
	@Column(name = "target_value", length = 320)
	private String targetValue;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "used_at")
	private Instant usedAt;

	/** Set when a resend supersedes this code, per specification section 14. */
	@Column(name = "invalidated_at")
	private Instant invalidatedAt;

	@Column(name = "attempt_count", nullable = false)
	private int attemptCount;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected VerificationToken() {
		// Required by JPA.
	}

	public VerificationToken(UUID userId, VerificationTokenType type, String tokenHash, Instant expiresAt) {
		this.userId = userId;
		this.type = type;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
	}

	/**
	 * A code is usable only while it is unused, not superseded by a resend, and
	 * not past its expiry. All three must hold; checking expiry alone would let a
	 * code be spent twice.
	 */
	public boolean isUsable(Instant now) {
		return usedAt == null && invalidatedAt == null && expiresAt.isAfter(now);
	}

	public void markUsed(Instant now) {
		this.usedAt = now;
	}

	public void markInvalidated(Instant now) {
		this.invalidatedAt = now;
	}

	public void recordFailedAttempt() {
		this.attemptCount++;
	}

	public UUID getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public VerificationTokenType getType() {
		return type;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public String getTargetValue() {
		return targetValue;
	}

	public void setTargetValue(String targetValue) {
		this.targetValue = targetValue;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getUsedAt() {
		return usedAt;
	}

	public Instant getInvalidatedAt() {
		return invalidatedAt;
	}

	public int getAttemptCount() {
		return attemptCount;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof VerificationToken token)) {
			return false;
		}
		return id != null && id.equals(token.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}