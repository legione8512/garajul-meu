package ro.garajulmeu.auth;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VerificationCodeGeneratorTest {

	private final VerificationCodeGenerator generator = new VerificationCodeGenerator();

	/**
	 * A thousand draws will include values below 100000, so this also proves the
	 * zero padding: an unpadded code would fail the six-digit pattern.
	 */
	@Test
	void alwaysProducesExactlySixDigits() {
		for (int i = 0; i < 1000; i++) {
			assertThat(generator.generate()).matches("\\d{6}");
		}
	}

	@Test
	void doesNotReturnTheSameCodeOverAndOver() {
		Set<String> codes = new HashSet<>();
		for (int i = 0; i < 200; i++) {
			codes.add(generator.generate());
		}

		assertThat(codes).hasSizeGreaterThan(190);
	}
}