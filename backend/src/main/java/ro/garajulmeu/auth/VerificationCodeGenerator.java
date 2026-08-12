package ro.garajulmeu.auth;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

/**
 * Produces the six-digit codes sent by email.
 *
 * <p>Uses {@link SecureRandom} rather than {@code Math.random()} or
 * {@code java.util.Random}: those are seeded predictably, and an attacker who
 * observes a few codes could work out the seed and compute the next one for
 * somebody else's account.
 */
@Component
public class VerificationCodeGenerator {

	private static final int EXCLUSIVE_BOUND = 1_000_000;

	private final SecureRandom random = new SecureRandom();

	/**
	 * Always six characters. The zero padding matters: returning "42" instead of
	 * "000042" would quietly shrink the code space from a million values to far
	 * fewer, because short codes would be far more likely than long ones.
	 */
	public String generate() {
		return String.format("%06d", random.nextInt(EXCLUSIVE_BOUND));
	}
}