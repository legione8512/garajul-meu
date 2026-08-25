package ro.garajulmeu.auth;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import ro.garajulmeu.common.RateLimitProperties;
import ro.garajulmeu.common.RateLimitProperties.Policy;
import ro.garajulmeu.common.RateLimiter;
import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;

/**
 * Applies the rate limit policies to the authentication endpoints and turns an
 * exhausted budget into {@code RATE_LIMITED}.
 *
 * <p>
 * Two keys are checked for every request, because either one alone is trivially
 * avoided. Counting only by address punishes everyone behind one corporate NAT
 * for the actions of a single person. Counting only by address <em>book</em>
 * entry lets an attacker rotate through addresses and never spend a budget at
 * all.
 *
 * <p>
 * <strong>{@code /auth/refresh} is deliberately not here, since
 * 2026-08-25.</strong> It was limited by address alone - the only key available
 * before the token is looked up - and a mobile application turns that from a
 * tolerable inaccuracy into a fault: an entire carrier leaves through a handful
 * of CGNAT addresses, every launch refreshes, and a ten-minute access token
 * means roughly six an hour per active user. Ten of them exhaust sixty, and the
 * eleventh meets RATE_LIMITED while restoring a session, which reads as a
 * broken application rather than as a limit. What the limit could not do in
 * exchange: stop token guessing, which is hopeless against 256 bits, or stop a
 * real attack, which would come from many addresses. Reuse detection already
 * revokes an entire family the moment a spent token reappears.
 * {@code AuthRateLimitTest} asserts the absence so nobody later reads it as an
 * oversight.
 */
@Component
class AuthRateLimit {

	private final RateLimiter limiter;

	private final RateLimitProperties properties;

	AuthRateLimit(RateLimiter limiter, RateLimitProperties properties) {
		this.limiter = limiter;
		this.properties = properties;
	}

	/** Login and email verification: each attempt costs an Argon2 comparison. */
	void credentialCheck(HttpServletRequest httpRequest, String email) {
		enforce("credential-check", properties.credentialCheck(), httpRequest, email);
	}

	/** Registration and resend: each attempt sends mail to a real inbox. */
	void emailDispatch(HttpServletRequest httpRequest, String email) {
		enforce("email-dispatch", properties.emailDispatch(), httpRequest, email);
	}

	/**
	 * Both keys, always. The email is no longer nullable: the one caller that
	 * passed null was the refresh endpoint, and with that gone a null here would be
	 * a mistake rather than a case worth handling.
	 */
	private void enforce(String policyName, Policy policy, HttpServletRequest httpRequest, String email) {
		// The network address is checked first, and an exhausted one stops here
		// without ever creating an entry for the email key. That ordering is what
		// bounds the limiter's own memory: one caller can only create as many
		// email keys as their address budget allows, so flooding us with distinct
		// addresses cannot grow the map without first spending that budget.
		if (!limiter.tryConsume(policyName + ":ip:" + httpRequest.getRemoteAddr(), policy)) {
			throw new ApiException(ErrorCode.RATE_LIMITED);
		}

		// The same normalisation the service uses. Without it "A@B.com" and
		// "a@b.com" would hold separate budgets, and changing capitalisation
		// would be enough to bypass the email key entirely.
		if (!limiter.tryConsume(policyName + ":email:" + AuthService.normalise(email), policy)) {
			throw new ApiException(ErrorCode.RATE_LIMITED);
		}
	}
}