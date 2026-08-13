package ro.garajulmeu.auth;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

/**
 * One link in a rotation chain. Specification section 10.9.
 *
 * <p>Every token issued from the same login shares a {@code familyId}. Refreshing
 * revokes the current link and issues the next one in the same family, so the
 * family is the session and a single row is one use of it.
 *
 * <p>Rows are never deleted on rotation. A spent token must remain findable -
 * that is precisely how a replay is detected.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	/** SHA-256 hex of the token. The token itself is never stored. */
	@Column(name = "token_hash", nullable = false, length = 64)
	private String tokenHash;

	@Column(name = "family_id", nullable = false)
	private UUID familyId;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	@Column(name = "replaced_by_token_id")
	private UUID replacedByTokenId;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected RefreshToken() {
		// Required by JPA.
	}

	public RefreshToken(UUID userId, String tokenHash, UUID familyId, Instant expiresAt) {
		this.userId = userId;
		this.tokenHash = tokenHash;
		this.familyId = familyId;
		this.expiresAt = expiresAt;
	}

	public boolean isUsable(Instant now) {
		return revokedAt == null && expiresAt.isAfter(now);
	}

	public void revokeAsRotated(Instant now, UUID successorId) {
		this.revokedAt = now;
		this.replacedByTokenId = successorId;
	}

	public UUID getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public UUID getFamilyId() {
		return familyId;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getRevokedAt() {
		return revokedAt;
	}

	public UUID getReplacedByTokenId() {
		return replacedByTokenId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof RefreshToken token)) {
			return false;
		}
		return id != null && id.equals(token.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}