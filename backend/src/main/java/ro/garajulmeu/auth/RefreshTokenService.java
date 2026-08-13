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
 * <p>The security property that matters is this: a legitimate client presents
 * each token exactly once. If a token that has already been rotated appears
 * again, two parties hold the same token - the original holder and a thief - so
 * the whole family is revoked and both are forced to log in again. A stolen
 * token therefore buys at most one refresh before it becomes an alarm.
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
			// This token was already exchanged. A legitimate client never replays
			// a spent token, so someone else holds a copy. Revoking the family
			// logs out the device entirely - deliberately disruptive, because the
			// alternative is letting a thief keep refreshing forever.
			repository.revokeFamily(current.getFamilyId(), now);
			log.warn("Refresh token reuse detected; revoked family {}", current.getFamilyId());
			throw new ApiException(ErrorCode.REFRESH_TOKEN_REUSED);
		}

		if (!current.getExpiresAt().isAfter(now)) {
			throw new ApiException(ErrorCode.REFRESH_TOKEN_INVALID);
		}

		IssuedRefreshToken next = issue(current.getUserId(), current.getFamilyId());
		current.revokeAsRotated(now, next.id());

		return next;
	}

	/** Ends the session the token belongs to. Unknown tokens are ignored. */
	@Transactional
	public void revokeSessionOf(String presentedToken) {
		repository.findByTokenHash(sha256(presentedToken))
				.ifPresent(token -> repository.revokeFamily(token.getFamilyId(), Instant.now()));
	}

	private IssuedRefreshToken issue(UUID userId, UUID familyId) {
		byte[] bytes = new byte[TOKEN_BYTES];
		random.nextBytes(bytes);
		String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

		Instant expiresAt = Instant.now().plus(properties.refreshTokenValidity());
		RefreshToken saved = repository.saveAndFlush(
				new RefreshToken(userId, sha256(value), familyId, expiresAt));

		return new IssuedRefreshToken(saved.getId(), value, expiresAt, familyId);
	}

	/**
	 * SHA-256 rather than Argon2. The token has 256 bits of entropy, so brute
	 * force is irrelevant, and a deterministic digest is what allows the unique
	 * index to find the row. Argon2's per-hash salt would force a full scan.
	 */
	static String sha256(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException unreachable) {
			throw new IllegalStateException("Every JVM is required to provide SHA-256", unreachable);
		}
	}

	/** {@code value} is the only moment the raw token exists outside the client. */
	public record IssuedRefreshToken(UUID id, String value, Instant expiresAt, UUID familyId) {
	}
}