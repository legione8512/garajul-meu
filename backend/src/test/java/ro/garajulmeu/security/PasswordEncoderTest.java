package ro.garajulmeu.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the encoder this application actually configures, not one constructed
 * in the test, so a change to SecurityConfig cannot silently weaken hashing.
 */
class PasswordEncoderTest {

	private final PasswordEncoder encoder = new SecurityConfig().passwordEncoder();

	@Test
	void hashesWithArgon2idAndNeverKeepsThePlainPassword() {
		String hash = encoder.encode("correct horse battery staple");

		assertThat(hash).startsWith("$argon2id$");
		assertThat(hash).doesNotContain("correct horse battery staple");
	}

	/**
	 * A per-password salt means two accounts with the same password produce
	 * different hashes, so a stolen database does not reveal which users share
	 * a password.
	 */
	@Test
	void producesADifferentHashEachTimeForTheSamePassword() {
		assertThat(encoder.encode("shared-password"))
				.isNotEqualTo(encoder.encode("shared-password"));
	}

	@Test
	void acceptsTheOriginalPasswordAndRejectsAnyOther() {
		String hash = encoder.encode("original-password");

		assertThat(encoder.matches("original-password", hash)).isTrue();
		assertThat(encoder.matches("Original-Password", hash)).isFalse();
		assertThat(encoder.matches("", hash)).isFalse();
	}
}