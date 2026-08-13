package ro.garajulmeu.security;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Builds the beans directly instead of starting Spring, so this runs in
 * milliseconds and still exercises exactly the configuration the application
 * uses.
 */
class AccessTokenServiceTest {

	private static final JwtProperties PROPERTIES = new JwtProperties(
			"a-test-signing-key-of-more-than-32-bytes", Duration.ofMinutes(10), "garajul-meu");

	private final SecurityConfig config = new SecurityConfig();

	private final AccessTokenService accessTokenService =
			new AccessTokenService(config.jwtEncoder(PROPERTIES), PROPERTIES);

	private final JwtDecoder decoder = config.jwtDecoder(PROPERTIES);

	@Test
	void issuesATokenOurOwnDecoderAccepts() {
		UUID accountId = UUID.randomUUID();

		Jwt decoded = decoder.decode(accessTokenService.issueFor(accountId).value());

		assertThat(decoded.getSubject()).isEqualTo(accountId.toString());
		// getIssuer() insists on a URL because it comes from OAuth2. RFC 7519
		// allows any StringOrURI, so read the raw claim instead.
		assertThat(decoded.getClaimAsString("iss")).isEqualTo("garajul-meu");
	}

	@Test
	void expiresWithinTheConfiguredWindow() {
		Instant before = Instant.now();

		AccessTokenService.IssuedAccessToken token = accessTokenService.issueFor(UUID.randomUUID());

		assertThat(token.expiresAt())
				.isAfter(before.plus(Duration.ofMinutes(9)))
				.isBefore(before.plus(Duration.ofMinutes(11)));
	}

	/** A token carries no personal data: it is only base64 and lives on the client. */
	@Test
	void carriesNothingButTheAccountIdentifier() {
		Jwt decoded = decoder.decode(accessTokenService.issueFor(UUID.randomUUID()).value());

		assertThat(decoded.getClaims()).containsOnlyKeys("iss", "iat", "exp", "sub");
	}

	@Test
	void rejectsATokenSignedWithADifferentKey() {
		JwtProperties otherKey = new JwtProperties(
				"a-completely-different-key-of-32-plus-bytes", Duration.ofMinutes(10), "garajul-meu");
		AccessTokenService impostor = new AccessTokenService(config.jwtEncoder(otherKey), otherKey);

		String forged = impostor.issueFor(UUID.randomUUID()).value();

		assertThatThrownBy(() -> decoder.decode(forged)).isInstanceOf(Exception.class);
	}
}