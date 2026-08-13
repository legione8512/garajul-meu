package ro.garajulmeu.common;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * How many attempts each class of endpoint allows, and over what window.
 *
 * <p>Policies are grouped by what a request costs us rather than by endpoint, so
 * the grouping explains itself: {@code credentialCheck} covers the endpoints
 * that pay for an Argon2 comparison, {@code emailDispatch} the ones that send
 * mail.
 *
 * @param credentialCheck login and email verification - each one costs an Argon2
 *                        comparison, roughly 50 ms and 16 MB, which is what
 *                        makes an unthrottled endpoint a cheap denial of service
 * @param emailDispatch   registration and resend - each one sends an email, so
 *                        an unthrottled endpoint is a way to use us as a mail
 *                        cannon aimed at someone else's inbox
 * @param tokenRefresh    rotation is cheap, a SHA-256 lookup, so the allowance
 *                        is generous. It exists to bound a runaway client, not
 *                        to defend against cost
 */
@ConfigurationProperties(prefix = "garajul-meu.rate-limit")
public record RateLimitProperties(Policy credentialCheck, Policy emailDispatch, Policy tokenRefresh) {

	public RateLimitProperties {
		credentialCheck = credentialCheck != null ? credentialCheck : new Policy(10, Duration.ofMinutes(15));
		emailDispatch = emailDispatch != null ? emailDispatch : new Policy(5, Duration.ofHours(1));
		tokenRefresh = tokenRefresh != null ? tokenRefresh : new Policy(60, Duration.ofHours(1));
	}

	/**
	 * The compact constructor refuses a nonsensical policy at startup rather than
	 * at the first request. Setting only {@code limit} in configuration and
	 * forgetting {@code window} would otherwise bind a null window and fail much
	 * later, in the middle of an attack, which is the worst possible moment.
	 */
	public record Policy(int limit, Duration window) {

		public Policy {
			if (limit <= 0 || window == null || window.isZero() || window.isNegative()) {
				throw new IllegalArgumentException(
						"A rate limit policy needs a positive limit and a positive window");
			}
		}
	}
}