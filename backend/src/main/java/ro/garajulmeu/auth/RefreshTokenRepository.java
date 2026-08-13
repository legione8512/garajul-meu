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
}