package ro.garajulmeu.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

	/**
	 * The newest code issued for one account and one purpose. Newest rather than
	 * any, because a resend leaves the previous rows behind for audit.
	 */
	Optional<VerificationToken> findFirstByUserIdAndTypeOrderByCreatedAtDesc(
			UUID userId, VerificationTokenType type);

	/**
	 * Supersedes every code still outstanding for one account and purpose.
	 * Specification section 14: a resend invalidates the prior code.
	 *
	 * <p>A bulk update rather than load-then-save, so a user who taps "resend"
	 * twice cannot end up with two codes both accepted.
	 */
	@Modifying
	@Query("""
			update VerificationToken token
			   set token.invalidatedAt = :now
			 where token.userId = :userId
			   and token.type = :type
			   and token.usedAt is null
			   and token.invalidatedAt is null
			""")
	int invalidateOutstandingCodes(@Param("userId") UUID userId,
			@Param("type") VerificationTokenType type,
			@Param("now") Instant now);
}