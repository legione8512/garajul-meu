package ro.garajulmeu.security;

import java.time.Duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Signing and lifetime of the access token. Specification section 14.
 *
 * @param secret               HMAC signing key. Deliberately has no default:
 *                             a missing secret must stop the application, never
 *                             fall back to something guessable. The minimum
 *                             length is the 256 bits HS256 requires - a shorter
 *                             key weakens the signature rather than failing
 *                             loudly.
 * @param accessTokenValidity  about ten minutes. Short, because the token cannot
 *                             be revoked once issued; the rotating refresh token
 *                             in 4.5 is what makes long sessions possible.
 */
@ConfigurationProperties(prefix = "garajul-meu.jwt")
@Validated
public record JwtProperties(

		@NotBlank
		@Size(min = 32)
		String secret,

		@DefaultValue("10m")
		Duration accessTokenValidity,

		@DefaultValue("garajul-meu")
		String issuer) {
}