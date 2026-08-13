package ro.garajulmeu.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	/**
	 * Revokes every still-live token in one family, in a single statement.
	 *
	 * <p>{@code flushAutomatically} makes a token persisted earlier in the same
	 * transaction visible to the UPDATE; {@code clearAutomatically} stops any
	 * entity already loaded from keeping its stale, unrevoked state. A bulk
	 * update goes straight to the database and knows nothing of the persistence
	 * context.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			update RefreshToken token
			   set token.revokedAt = :now
			 where token.familyId = :familyId
			   and token.revokedAt is null
			""")
	int revokeFamily(@Param("familyId") UUID familyId, @Param("now") Instant now);
	
	/**
	 * Ends every session the account has anywhere, in one statement.
	 *
	 * <p>Specification section 14 requires this after a password reset: whoever
	 * knew the old password may be the reason the reset was needed, and leaving
	 * their sessions alive would defeat the whole exercise.
	 *
	 * <p>Same {@code flushAutomatically} / {@code clearAutomatically} pairing and
	 * the same warning as above: **any entity loaded before this call is stale
	 * afterwards.** Callers must finish with their entities before invoking it.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			update RefreshToken token
			   set token.revokedAt = :now
			 where token.userId = :userId
			   and token.revokedAt is null
			""")
	int revokeAllForUser(@Param("userId") UUID userId, @Param("now") Instant now);
}