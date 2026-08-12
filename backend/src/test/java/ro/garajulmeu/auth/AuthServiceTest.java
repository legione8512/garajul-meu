package ro.garajulmeu.auth;

import java.util.UUID;

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
}