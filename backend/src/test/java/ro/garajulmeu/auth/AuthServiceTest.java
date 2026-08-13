package ro.garajulmeu.auth;

import java.util.UUID;
import java.time.Instant;

import ro.garajulmeu.auth.dto.ResendVerificationRequest;
import ro.garajulmeu.auth.dto.VerifyEmailRequest;
import static org.mockito.Mockito.verifyNoInteractions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import ro.garajulmeu.TestcontainersConfiguration;
import ro.garajulmeu.auth.dto.RegisterRequest;
import ro.garajulmeu.email.EmailProvider;
import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;
import ro.garajulmeu.user.Language;
import ro.garajulmeu.user.User;
import ro.garajulmeu.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthServiceTest {

	private static final String PASSWORD = "a-sufficiently-long-password";

	@Autowired
	private AuthService authService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private VerificationTokenRepository tokenRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	/** Replaces the logging provider so the emailed code can be inspected. */
	@MockitoBean
	private EmailProvider emailProvider;

	@Test
	void storesTheAddressNormalisedAndThePasswordHashed() {
		authService.register(new RegisterRequest(
				"Marius Robert", "  Marius.Robert@Example.COM  ", PASSWORD, "ro"));

		User saved = userRepository.findByEmail("marius.robert@example.com").orElseThrow();
		assertThat(saved.getPasswordHash()).startsWith("$argon2id$");
		assertThat(saved.getPasswordHash()).doesNotContain(PASSWORD);
		assertThat(saved.isEmailVerified()).isFalse();
	}

	@Test
	void emailsExactlyTheCodeWhoseHashIsStored() {
		authService.register(new RegisterRequest("Marius Robert", "codes@example.com", PASSWORD, "ro"));

		ArgumentCaptor<String> emailedCode = ArgumentCaptor.forClass(String.class);
		verify(emailProvider).sendVerificationCode(eq("codes@example.com"), emailedCode.capture(), eq(Language.RO));

		UUID userId = userRepository.findByEmail("codes@example.com").orElseThrow().getId();
		VerificationToken stored = tokenRepository
				.findFirstByUserIdAndTypeOrderByCreatedAtDesc(userId, VerificationTokenType.EMAIL_VERIFICATION)
				.orElseThrow();

		assertThat(emailedCode.getValue()).matches("\\d{6}");
		assertThat(stored.getTokenHash()).doesNotContain(emailedCode.getValue());
		assertThat(passwordEncoder.matches(emailedCode.getValue(), stored.getTokenHash())).isTrue();
	}

	@Test
	void refusesAnAddressAlreadyTakenEvenInDifferentCase() {
		authService.register(new RegisterRequest("First Owner", "taken@example.com", PASSWORD, "ro"));

		assertThatThrownBy(() -> authService.register(
				new RegisterRequest("Second Owner", "TAKEN@Example.com", PASSWORD, "ro")))
				.isInstanceOf(ApiException.class)
				.extracting(thrown -> ((ApiException) thrown).errorCode())
				.isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
	}

	@Test
	void defaultsToRomanianWhenTheClientSendsNoLanguage() {
		authService.register(new RegisterRequest("Marius Robert", "nolang@example.com", PASSWORD, null));

		assertThat(userRepository.findByEmail("nolang@example.com").orElseThrow().getPreferredLanguage())
				.isEqualTo(Language.RO);
	}
	private String registerAndCaptureCode(String email) {
		authService.register(new RegisterRequest("Marius Robert", email, PASSWORD, "ro"));

		ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
		verify(emailProvider).sendVerificationCode(eq(email), code.capture(), eq(Language.RO));
		return code.getValue();
	}

	@Test
	void marksTheAccountVerifiedWhenTheCodeIsCorrect() {
		String code = registerAndCaptureCode("verify@example.com");

		authService.verifyEmail(new VerifyEmailRequest("verify@example.com", code));

		assertThat(userRepository.findByEmail("verify@example.com").orElseThrow().isEmailVerified()).isTrue();
	}

	@Test
	void countsAWrongAttemptInsteadOfSilentlyDiscardingIt() {
		registerAndCaptureCode("wrong@example.com");

		assertThatThrownBy(() -> authService.verifyEmail(new VerifyEmailRequest("wrong@example.com", "000000")))
				.isInstanceOf(ApiException.class)
				.extracting(thrown -> ((ApiException) thrown).errorCode())
				.isEqualTo(ErrorCode.VERIFICATION_CODE_INVALID);

		UUID userId = userRepository.findByEmail("wrong@example.com").orElseThrow().getId();
		assertThat(tokenRepository
				.findFirstByUserIdAndTypeOrderByCreatedAtDesc(userId, VerificationTokenType.EMAIL_VERIFICATION)
				.orElseThrow()
				.getAttemptCount()).isEqualTo(1);
	}

	@Test
	void cannotSpendTheSameCodeTwice() {
		String code = registerAndCaptureCode("once@example.com");
		authService.verifyEmail(new VerifyEmailRequest("once@example.com", code));

		assertThatThrownBy(() -> authService.verifyEmail(new VerifyEmailRequest("once@example.com", code)))
				.isInstanceOf(ApiException.class)
				.extracting(thrown -> ((ApiException) thrown).errorCode())
				.isEqualTo(ErrorCode.VERIFICATION_CODE_INVALID);
	}

	/** Distinct from INVALID so the client can offer "resend" rather than "retry". */
	@Test
	void reportsAnExpiredCodeDistinctly() {
		User user = userRepository.saveAndFlush(
				new User("Marius Robert", "expired@example.com", "argon2-placeholder"));
		tokenRepository.saveAndFlush(new VerificationToken(
				user.getId(),
				VerificationTokenType.EMAIL_VERIFICATION,
				passwordEncoder.encode("123456"),
				Instant.now().minusSeconds(60)));

		assertThatThrownBy(() -> authService.verifyEmail(new VerifyEmailRequest("expired@example.com", "123456")))
				.isInstanceOf(ApiException.class)
				.extracting(thrown -> ((ApiException) thrown).errorCode())
				.isEqualTo(ErrorCode.VERIFICATION_CODE_EXPIRED);
	}

	@Test
	void aResendInvalidatesTheEarlierCodeAndTheOldOneNoLongerWorks() {
		String firstCode = registerAndCaptureCode("resend@example.com");
		UUID userId = userRepository.findByEmail("resend@example.com").orElseThrow().getId();
		UUID firstTokenId = tokenRepository
				.findFirstByUserIdAndTypeOrderByCreatedAtDesc(userId, VerificationTokenType.EMAIL_VERIFICATION)
				.orElseThrow()
				.getId();

		authService.resendVerificationCode(new ResendVerificationRequest("resend@example.com"));

		assertThat(tokenRepository.findById(firstTokenId).orElseThrow().getInvalidatedAt())
				.as("the superseded code must be marked invalidated, not merely shadowed by a newer one")
				.isNotNull();

		assertThatThrownBy(() -> authService.verifyEmail(new VerifyEmailRequest("resend@example.com", firstCode)))
				.isInstanceOf(ApiException.class)
				.extracting(thrown -> ((ApiException) thrown).errorCode())
				.isEqualTo(ErrorCode.VERIFICATION_CODE_INVALID);
	}
	/** Answering differently would turn resend into an account-enumeration tool. */
	@Test
	void staysSilentWhenAResendIsRequestedForAnUnknownAddress() {
		authService.resendVerificationCode(new ResendVerificationRequest("nobody@example.com"));

		verifyNoInteractions(emailProvider);
	}
}