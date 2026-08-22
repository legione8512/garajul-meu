package ro.garajulmeu.user;

import java.util.UUID;

import org.springframework.stereotype.Component;

import ro.garajulmeu.common.RateLimitProperties;
import ro.garajulmeu.common.RateLimitProperties.Policy;
import ro.garajulmeu.common.RateLimiter;
import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;

/**
 * Applies the rate limit policies to the authenticated account endpoints and
 * turns an exhausted budget into {@code RATE_LIMITED}.
 *
 * <p><strong>One key, and it is the account.</strong> {@link
 * ro.garajulmeu.auth.AuthRateLimit} checks two - address and email - because
 * either one alone is trivially avoided by callers it cannot identify. Here
 * there is nothing to avoid: the caller has already presented an unexpired
 * access token for exactly this account, and the subject of that token is the
 * strongest identity this application ever has. Adding the network address as a
 * second key would only reintroduce the problem the auth limiter tolerates out
 * of necessity - punishing everyone behind one corporate NAT for the actions of
 * one person - while buying nothing, because an authenticated caller cannot
 * rotate their account id the way an anonymous one rotates addresses.
 *
 * <p>That also bounds the memory without needing the ordering trick the auth
 * limiter uses. An entry can only be created by someone holding a valid token,
 * so the number of live keys is bounded by the number of real accounts rather
 * than by what an attacker can invent.
 *
 * <p><strong>The key prefixes deliberately differ from the auth limiter's.</strong>
 * Sharing {@code credential-check:} would mean anonymous login attempts and an
 * authenticated password change drew on one budget - so a stranger failing to
 * log in could lock the owner out of changing their own password, and the owner
 * could exhaust the budget that protects them. Same policy numbers, separate
 * counters, because they are separate callers.
 *
 * <p>The policies themselves are reused rather than invented. They are grouped
 * by what a request costs, and these requests cost exactly what the auth
 * endpoints cost: an Argon2 comparison, or an email to a real inbox.
 */
@Component
class AccountRateLimit {

	private final RateLimiter limiter;

	private final RateLimitProperties properties;

	AccountRateLimit(RateLimiter limiter, RateLimitProperties properties) {
		this.limiter = limiter;
		this.properties = properties;
	}

	/**
	 * The password change, the email-change confirmation and the account
	 * deletion: each pays at least one Argon2 comparison, and the password
	 * change pays two, since it also encodes the replacement.
	 */
	void credentialCheck(UUID accountId) {
		enforce("account-credential-check", properties.credentialCheck(), accountId);
	}

	/**
	 * Requesting an email change, which sends a code to a real inbox. Limited
	 * more tightly than the hash it also pays for, because the scarce resource
	 * is our sending reputation rather than our CPU.
	 */
	void emailDispatch(UUID accountId) {
		enforce("account-email-dispatch", properties.emailDispatch(), accountId);
	}

	private void enforce(String policyName, Policy policy, UUID accountId) {
		if (!limiter.tryConsume(policyName + ":" + accountId, policy)) {
			throw new ApiException(ErrorCode.RATE_LIMITED);
		}
	}
}