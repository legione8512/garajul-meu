package ro.garajulmeu.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;

/**
 * Issues, rotates and revokes refresh tokens. Specification section 14.
 *
 * <p>
 * The security property that matters is this: a legitimate client presents each
 * token exactly once. If a token that has already been rotated appears again,
 * two parties hold the same token - the original holder and a thief - so the
 * whole family is revoked and both are forced to log in again. A stolen token
 * therefore buys at most one refresh before it becomes an alarm.
 *
 * <p>
 * <strong>With one exception, added 2026-08-31 because the rule as written
 * punished an ordinary accident.</strong> A client can present a spent token
 * without ever having held two: it rotated, and the response carrying the
 * replacement never arrived - a navigation that aborted the request, a dropped
 * mobile connection, two tabs restored at once, a WebView suspended mid-flight.
 * The server has moved on and the client has not, which is indistinguishable
 * from theft by the rule above. It was hit twice in one afternoon of ordinary
 * clicking, and the cost was being signed out of every device.
 *
 * <p>
 * So a replay is forgiven when all three of these hold: the token was spent
 * <em>by rotation</em> rather than by a logout or a family revocation; that
 * happened within {@code refreshReuseGrace}; and the replacement it points at
 * has never itself been used. Anything else is the old behaviour exactly.
 *
 * <p>
 * <strong>Why this does not weaken the alarm.</strong> A thief replaying inside
 * the window does get a token - and the victim's next refresh, minutes later,
 * lands outside the window and revokes the family. The theft is still caught;
 * it is caught one step later. What is traded away is a few seconds in which an
 * attacker who wins a race gets one extra token. What is bought is that a lost
 * response no longer ends every session the account has.
 */
@Service
public class RefreshTokenService {

	private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

	/** 256 bits, the same order of magnitude as the signing key. */
	private static final int TOKEN_BYTES = 32;

	private final RefreshTokenRepository repository;

	private final AuthProperties properties;

	private final SecureRandom random = new SecureRandom();

	RefreshTokenService(RefreshTokenRepository repository, AuthProperties properties) {
		this.repository = repository;
		this.properties = properties;
	}

	/** Starts a new family: one login, on one device. */
	@Transactional
	public IssuedRefreshToken startFamily(UUID userId) {
		return issue(userId, UUID.randomUUID());
	}

	@Transactional
	public IssuedRefreshToken rotate(String presentedToken) {
		RefreshToken current = repository.findByTokenHash(sha256(presentedToken))
				.orElseThrow(() -> new ApiException(ErrorCode.REFRESH_TOKEN_INVALID));

		Instant now = Instant.now();

		if (current.getRevokedAt() != null) {
			RefreshToken undelivered = replacementNeverCollected(current, now);

			if (undelivered == null) {
				// This token was already exchanged and the replacement was
				// plainly collected. A legitimate client never replays that, so
				// someone else holds a copy. Revoking the family logs out the
				// device entirely - deliberately disruptive, because the
				// alternative is letting a thief keep refreshing forever.
				repository.revokeFamily(current.getFamilyId(), now);
				log.warn("Refresh token reuse detected; revoked family {}", current.getFamilyId());
				throw new ApiException(ErrorCode.REFRESH_TOKEN_REUSED);
			}

			// Logged, and at INFO rather than WARN: this is an accident the
			// design now expects, not an incident. It stays visible because a
			// sudden run of these means something is dropping responses.
			log.info("Rotation replacement was never collected; re-issuing in family {}", current.getFamilyId());

			IssuedRefreshToken reissued = issue(undelivered.getUserId(), undelivered.getFamilyId());
			undelivered.revokeAsRotated(now, reissued.id());

			return reissued;
		}

		if (!current.getExpiresAt().isAfter(now)) {
			throw new ApiException(ErrorCode.REFRESH_TOKEN_INVALID);
		}

		IssuedRefreshToken next = issue(current.getUserId(), current.getFamilyId());
		current.revokeAsRotated(now, next.id());

		return next;
	}

	/**
	 * The replacement of a token whose successor plainly never reached anybody, or
	 * {@code null} when the replay is what it looks like.
	 *
	 * <p>
	 * Three conditions, and each one is load-bearing.
	 *
	 * <p>
	 * {@code replacedByTokenId} being set is what separates <em>rotated</em> from
	 * <em>revoked</em>. A bulk revocation - logout, a password reset, an earlier
	 * reuse alarm - writes only {@code revokedAt}, so a token killed that way can
	 * never take this path. That distinction was already in the data before this
	 * method existed.
	 *
	 * <p>
	 * The window keeps the forgiveness to the length of a lost request rather than
	 * the length of a session.
	 *
	 * <p>
	 * The successor being unused is the one that preserves the alarm. If the
	 * replacement <em>was</em> collected and used, then two parties really do hold
	 * live tokens, and that is the case this must not forgive.
	 */
	private RefreshToken replacementNeverCollected(RefreshToken presented, Instant now) {
		if (presented.getReplacedByTokenId() == null) {
			return null;
		}

		if (presented.getRevokedAt().isBefore(now.minus(properties.refreshReuseGrace()))) {
			return null;
		}

		return repository.findById(presented.getReplacedByTokenId()).filter(successor -> successor.isUsable(now))
				.orElse(null);
	}

	/** Ends the session the token belongs to. Unknown tokens are ignored. */
	@Transactional
	public void revokeSessionOf(String presentedToken) {
		repository.findByTokenHash(sha256(presentedToken))
				.ifPresent(token -> repository.revokeFamily(token.getFamilyId(), Instant.now()));
	}

	/** Ends every session for an account, on every device. */
	@Transactional
	public void revokeAllSessionsOf(UUID userId) {
		int revoked = repository.revokeAllForUser(userId, Instant.now());
		log.info("Revoked {} refresh token(s) for account {}", revoked, userId);
	}

	private IssuedRefreshToken issue(UUID userId, UUID familyId) {
		byte[] bytes = new byte[TOKEN_BYTES];
		random.nextBytes(bytes);
		String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

		Instant expiresAt = Instant.now().plus(properties.refreshTokenValidity());
		RefreshToken saved = repository.saveAndFlush(new RefreshToken(userId, sha256(value), familyId, expiresAt));

		return new IssuedRefreshToken(saved.getId(), userId, value, expiresAt, familyId);

	}

	/**
	 * SHA-256 rather than Argon2. The token has 256 bits of entropy, so brute force
	 * is irrelevant, and a deterministic digest is what allows the unique index to
	 * find the row. Argon2's per-hash salt would force a full scan.
	 */
	static String sha256(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException unreachable) {
			throw new IllegalStateException("Every JVM is required to provide SHA-256", unreachable);
		}
	}

	/** {@code value} is the only moment the raw token exists outside the client. */
	public record IssuedRefreshToken(UUID id, UUID userId, String value, Instant expiresAt, UUID familyId) {
	}
}