package ro.garajulmeu.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import ro.garajulmeu.TestcontainersConfiguration;
import ro.garajulmeu.user.User;
import ro.garajulmeu.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class VerificationTokenRepositoryTest {

	@Autowired
	private VerificationTokenRepository tokenRepository;

	@Autowired
	private UserRepository userRepository;

	private UUID givenUser(String email) {
		return userRepository.saveAndFlush(new User("Marius Robert", email, "argon2-placeholder")).getId();
	}

	private VerificationToken givenCode(UUID userId, Instant expiresAt) {
		return tokenRepository.saveAndFlush(new VerificationToken(
				userId, VerificationTokenType.EMAIL_VERIFICATION, "argon2-hash-of-code", expiresAt));
	}

	@Test
	void aFreshCodeIsUsable() {
		VerificationToken code = givenCode(givenUser("fresh@example.com"),
				Instant.now().plus(15, ChronoUnit.MINUTES));

		assertThat(code.isUsable(Instant.now())).isTrue();
		assertThat(code.getAttemptCount()).isZero();
	}

	@Test
	void anExpiredCodeIsNotUsable() {
		VerificationToken code = givenCode(givenUser("expired@example.com"),
				Instant.now().minus(1, ChronoUnit.MINUTES));

		assertThat(code.isUsable(Instant.now())).isFalse();
	}

	@Test
	void aSpentCodeCannotBeUsedAgain() {
		VerificationToken code = givenCode(givenUser("spent@example.com"),
				Instant.now().plus(15, ChronoUnit.MINUTES));

		code.markUsed(Instant.now());

		assertThat(code.isUsable(Instant.now())).isFalse();
	}

	@Test
	void resendingSupersedesEveryOutstandingCode() {
		UUID userId = givenUser("resend@example.com");
		Instant future = Instant.now().plus(15, ChronoUnit.MINUTES);
		givenCode(userId, future);
		givenCode(userId, future);

		int superseded = tokenRepository.invalidateOutstandingCodes(
				userId, VerificationTokenType.EMAIL_VERIFICATION, Instant.now());

		assertThat(superseded).isEqualTo(2);
	}

	@Test
	void codesOfAnotherPurposeAreLeftAlone() {
		UUID userId = givenUser("purposes@example.com");
		tokenRepository.saveAndFlush(new VerificationToken(userId,
				VerificationTokenType.PASSWORD_RESET, "argon2-hash-of-code",
				Instant.now().plus(15, ChronoUnit.MINUTES)));

		int superseded = tokenRepository.invalidateOutstandingCodes(
				userId, VerificationTokenType.EMAIL_VERIFICATION, Instant.now());

		assertThat(superseded).isZero();
	}
}