package ro.garajulmeu.auth;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Tunable authentication limits, so a change of policy is a configuration
 * change rather than a code change.
 *
 * @param verificationCodeValidity how long a six-digit code stays usable
 * @param maxVerificationAttempts  wrong guesses allowed before a code is burnt,
 *                                 which is what makes a million-value code safe
 * @param refreshTokenValidity     about thirty days. Long is acceptable because
 *                                 the token rotates on every use and a stolen
 *                                 one is detected the moment either copy is
 *                                 presented twice.
 * @param refreshReuseGrace        how long a just-rotated token is still
 *                                 forgiven, for the client that never received
 *                                 its replacement. Seconds, not minutes: the
 *                                 window is the whole safety margin, and the
 *                                 reasoning for its size is in
 *                                 RefreshTokenService.
 */
@ConfigurationProperties(prefix = "garajul-meu.auth")
public record AuthProperties(@DefaultValue("15m") Duration verificationCodeValidity,
		@DefaultValue("5") int maxVerificationAttempts, @DefaultValue("30d") Duration refreshTokenValidity,
		@DefaultValue("15s") Duration refreshReuseGrace) {
}