package ro.garajulmeu.common;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * How many attempts each class of endpoint allows, and over what window.
 *
 * <p>
 * Policies are grouped by what a request costs us rather than by endpoint, so
 * the grouping explains itself: {@code credentialCheck} covers the endpoints
 * that pay for an Argon2 comparison, {@code emailDispatch} the ones that send
 * mail.
 *
 * <p>
 * <strong>There is no policy for token rotation, and its absence is a
 * decision.</strong> One existed until 2026-08-25 and was removed with the
 * endpoint's limit; the reasoning is on {@code AuthRateLimit}. Removing the
 * record component and the two YAML blocks together was deliberate - leaving
 * either behind would have produced configuration that looks live and binds to
 * nothing, which is precisely how three files of phase 16.3 went missing from a
 * jar while every test passed.
 *
 * @param credentialCheck login and email verification - each one costs an
 *                        Argon2 comparison, roughly 50 ms and 16 MB, which is
 *                        what makes an unthrottled endpoint a cheap denial of
 *                        service
 * @param emailDispatch   registration and resend - each one sends an email, so
 *                        an unthrottled endpoint is a way to use us as a mail
 *                        cannon aimed at someone else's inbox
 */
@ConfigurationProperties(prefix = "garajul-meu.rate-limit")
public record RateLimitProperties(Policy credentialCheck, Policy emailDispatch) {

	public RateLimitProperties {
		credentialCheck = credentialCheck != null ? credentialCheck : new Policy(10, Duration.ofMinutes(15));
		emailDispatch = emailDispatch != null ? emailDispatch : new Policy(5, Duration.ofHours(1));
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
				throw new IllegalArgumentException("A rate limit policy needs a positive limit and a positive window");
			}
		}
	}
}