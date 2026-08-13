package ro.garajulmeu.auth;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import ro.garajulmeu.TestcontainersConfiguration;
import ro.garajulmeu.auth.dto.ForgotPasswordRequest;
import ro.garajulmeu.auth.dto.ResetPasswordRequest;
import ro.garajulmeu.email.EmailProvider;
import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;
import ro.garajulmeu.user.User;
import ro.garajulmeu.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class PasswordResetServiceTest {

	private static final String OLD_PASSWORD = "the-original-long-password";

	private static final String NEW_PASSWORD = "a-brand-new-long-password";

	@Autowired
	private AuthService authService;

	@Autowired
	private RefreshTokenService refreshTokenService;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private VerificationTokenRepository tokenRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@MockitoBean
	private EmailProvider emailProvider;

	private User account(String email, boolean verified) {
		User user = new User("Marius Robert", email, passwordEncoder.encode(OLD_PASSWORD));
		if (verified) {
			user.setEmailVerifiedAt(Instant.now());
		}
		return userRepository.saveAndFlush(user);
	}

	/** The most recently emailed code, so a test never has to guess it. */
	private String emailedCode() {
		ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
		verify(emailProvider, atLeastOnce()).sendPasswordResetCode(anyString(), code.capture(), any());
		return code.getValue();
	}

	/** Specification section 14: this endpoint must not reveal who has an account. */
	@Test
	void aRequestForAnUnknownAddressSendsNothing() {
		authService.forgotPassword(new ForgotPasswordRequest("nobody@example.com"));

		verifyNoInteractions(emailProvider);
	}

	@Test
	void theEmailedCodeReplacesThePassword() {
		User user = account("reset@example.com", true);

		authService.forgotPassword(new ForgotPasswordRequest(user.getEmail()));
		authService.resetPassword(new ResetPasswordRequest(user.getEmail(), emailedCode(), NEW_PASSWORD));

		User stored = userRepository.findByEmail(user.getEmail()).orElseThrow();
		assertThat(passwordEncoder.matches(NEW_PASSWORD, stored.getPasswordHash())).isTrue();
		assertThat(passwordEncoder.matches(OLD_PASSWORD, stored.getPasswordHash())).isFalse();
	}

	@Test
	void aWrongCodeIsCountedAndLeavesThePasswordAlone() {
		User user = account("wrong-code@example.com", true);
		authService.forgotPassword(new ForgotPasswordRequest(user.getEmail()));

		assertThatThrownBy(() -> authService.resetPassword(
				new ResetPasswordRequest(user.getEmail(), "000000", NEW_PASSWORD)))
				.isInstanceOf(ApiException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.VERIFICATION_CODE_INVALID);

		User stored = userRepository.findByEmail(user.getEmail()).orElseThrow();
		assertThat(passwordEncoder.matches(OLD_PASSWORD, stored.getPasswordHash())).isTrue();

		VerificationToken token = tokenRepository
				.findFirstByUserIdAndTypeOrderByCreatedAtDesc(user.getId(), VerificationTokenType.PASSWORD_RESET)
				.orElseThrow();
		assertThat(token.getAttemptCount()).isEqualTo(1);
	}

	/**
	 * Distinct from INVALID because the caller demonstrably held a valid code for
	 * this address, so nothing is disclosed - and the client needs the difference
	 * to offer "send a new code" rather than "try again".
	 */
	@Test
	void anExpiredCodeAnswersItsOwnError() {
		User user = account("expired@example.com", true);
		tokenRepository.saveAndFlush(new VerificationToken(
				user.getId(), VerificationTokenType.PASSWORD_RESET,
				passwordEncoder.encode("123456"), Instant.now().minusSeconds(60)));

		assertThatThrownBy(() -> authService.resetPassword(
				new ResetPasswordRequest(user.getEmail(), "123456", NEW_PASSWORD)))
				.isInstanceOf(ApiException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.VERIFICATION_CODE_EXPIRED);
	}

	@Test
	void aSpentCodeCannotBeUsedTwice() {
		User user = account("replay@example.com", true);
		authService.forgotPassword(new ForgotPasswordRequest(user.getEmail()));
		String code = emailedCode();

		authService.resetPassword(new ResetPasswordRequest(user.getEmail(), code, NEW_PASSWORD));

		assertThatThrownBy(() -> authService.resetPassword(
				new ResetPasswordRequest(user.getEmail(), code, "yet-another-long-password")))
				.isInstanceOf(ApiException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.VERIFICATION_CODE_INVALID);
	}

	@Test
	void askingAgainInvalidatesTheEarlierCode() {
		User user = account("reissue@example.com", true);

		authService.forgotPassword(new ForgotPasswordRequest(user.getEmail()));
		String first = emailedCode();

		authService.forgotPassword(new ForgotPasswordRequest(user.getEmail()));

		assertThatThrownBy(() -> authService.resetPassword(
				new ResetPasswordRequest(user.getEmail(), first, NEW_PASSWORD)))
				.isInstanceOf(ApiException.class);
	}

	/**
	 * Specification section 14. Whoever knew the old password may be the reason
	 * the reset was needed, so every session ends - not only the one that asked.
	 */
	@Test
	void resettingEndsEverySessionOnEveryDevice() {
		User user = account("sessions@example.com", true);
		var phone = refreshTokenService.startFamily(user.getId());
		var laptop = refreshTokenService.startFamily(user.getId());

		authService.forgotPassword(new ForgotPasswordRequest(user.getEmail()));
		authService.resetPassword(new ResetPasswordRequest(user.getEmail(), emailedCode(), NEW_PASSWORD));

		// Re-read: revokeAllForUser is a bulk update, so anything loaded earlier
		// is stale by now.
		assertThat(refreshTokenRepository.findByTokenHash(RefreshTokenService.sha256(phone.value()))
				.orElseThrow().getRevokedAt()).isNotNull();
		assertThat(refreshTokenRepository.findByTokenHash(RefreshTokenService.sha256(laptop.value()))
				.orElseThrow().getRevokedAt()).isNotNull();
	}

	/**
	 * Entering a code that arrived by email proves the same thing verification
	 * proves. Without this, an account stuck unverified would reset successfully
	 * and still be refused at login, with nothing explaining why.
	 */
	@Test
	void resettingAlsoVerifiesAnAddressThatWasNeverVerified() {
		User user = account("never-verified@example.com", false);
		assertThat(user.isEmailVerified()).isFalse();

		authService.forgotPassword(new ForgotPasswordRequest(user.getEmail()));
		authService.resetPassword(new ResetPasswordRequest(user.getEmail(), emailedCode(), NEW_PASSWORD));

		assertThat(userRepository.findByEmail(user.getEmail()).orElseThrow().isEmailVerified()).isTrue();
	}

	/** An address that already exists must not be discoverable by timing either. */
	@Test
	void anAlreadyVerifiedAccountStaysVerified() {
		User user = account("stays@example.com", true);

		authService.forgotPassword(new ForgotPasswordRequest(user.getEmail()));
		authService.resetPassword(new ResetPasswordRequest(user.getEmail(), emailedCode(), NEW_PASSWORD));

		assertThat(userRepository.findByEmail(user.getEmail()).orElseThrow().isEmailVerified()).isTrue();
	}
}