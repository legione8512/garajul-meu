package ro.garajulmeu.auth;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

import ro.garajulmeu.common.RateLimitProperties;
import ro.garajulmeu.common.RateLimitProperties.Policy;
import ro.garajulmeu.common.RateLimiter;
import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;

/**
 * Applies the rate limit policies to the authentication endpoints and turns an
 * exhausted budget into {@code RATE_LIMITED}.
 *
 * <p>Two keys are checked for every request, because either one alone is
 * trivially avoided. Counting only by address punishes everyone behind one
 * corporate NAT for the actions of a single person. Counting only by address
 * <em>book</em> entry lets an attacker rotate through addresses and never spend
 * a budget at all.
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

	/** Rotation, which has no address to key on until after the token is read. */
	void tokenRefresh(HttpServletRequest httpRequest) {
		enforce("token-refresh", properties.tokenRefresh(), httpRequest, null);
	}

	private void enforce(String policyName, Policy policy, HttpServletRequest httpRequest, String email) {
		// The network address is checked first, and an exhausted one stops here
		// without ever creating an entry for the email key. That ordering is what
		// bounds the limiter's own memory: one caller can only create as many
		// email keys as their address budget allows, so flooding us with distinct
		// addresses cannot grow the map without first spending that budget.
		if (!limiter.tryConsume(policyName + ":ip:" + httpRequest.getRemoteAddr(), policy)) {
			throw new ApiException(ErrorCode.RATE_LIMITED);
		}

		if (email == null) {
			return;
		}

		// The same normalisation the service uses. Without it "A@B.com" and
		// "a@b.com" would hold separate budgets, and changing capitalisation
		// would be enough to bypass the email key entirely.
		if (!limiter.tryConsume(policyName + ":email:" + AuthService.normalise(email), policy)) {
			throw new ApiException(ErrorCode.RATE_LIMITED);
		}
	}
}