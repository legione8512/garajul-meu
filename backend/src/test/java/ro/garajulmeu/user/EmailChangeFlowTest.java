package ro.garajulmeu.user;

import java.time.Instant;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import ro.garajulmeu.TestcontainersConfiguration;
import ro.garajulmeu.email.EmailProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Annotations identical to AuthFlowTest, so the cached context is reused and no
 * further PostgreSQL container starts. Here the EmailProvider mock is genuinely
 * needed: the code is stored as an Argon2 hash and is otherwise unreadable.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class EmailChangeFlowTest {

	private static final String OLD_EMAIL = "old-address@example.com";

	private static final String NEW_EMAIL = "new-address@example.com";

	private static final String PASSWORD = "the-original-long-password";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@MockitoBean
	private EmailProvider emailProvider;

	@BeforeEach
	void givenAVerifiedAccount() {
		account(OLD_EMAIL);
	}

	private User account(String email) {
		User user = new User("Marius Robert", email, passwordEncoder.encode(PASSWORD));
		user.setEmailVerifiedAt(Instant.now());
		return userRepository.saveAndFlush(user);
	}

	private String accessTokenFor(String email) throws Exception {
		String body = mockMvc.perform(post("/api/v1/auth/login")
						.contentType("application/json")
						.content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		return JsonPath.read(body, "$.accessToken");
	}

	private String refreshTokenFor(String email) throws Exception {
		String body = mockMvc.perform(post("/api/v1/auth/login")
						.contentType("application/json")
						.content("{\"email\":\"%s\",\"password\":\"%s\",\"refreshTokenInBody\":true}"
								.formatted(email, PASSWORD)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		return JsonPath.read(body, "$.refreshToken");
	}

	/** Asks for the change and returns the code that was emailed. */
	private String requestChangeAndCaptureCode(String token) throws Exception {
		mockMvc.perform(post("/api/v1/users/me/change-email")
						.header("Authorization", "Bearer " + token)
						.contentType("application/json")
						.content("{\"newEmail\":\"%s\",\"currentPassword\":\"%s\"}"
								.formatted(NEW_EMAIL, PASSWORD)))
				.andExpect(status().isNoContent());

		ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
		verify(emailProvider).sendEmailChangeCode(eq(OLD_EMAIL), eq(NEW_EMAIL), code.capture(), any());
		return code.getValue();
	}

	/**
	 * The property the whole design rests on. A stolen access token can ask for the
	 * change, but the answer lands in an inbox the thief does not hold.
	 */
	@Test
	void theCodeGoesToTheOldAddressAndNamesTheNewOne() throws Exception {
		requestChangeAndCaptureCode(accessTokenFor(OLD_EMAIL));

		verify(emailProvider, never()).sendEmailChangeCode(eq(NEW_EMAIL), any(), any(), any());
	}

	@Test
	void nothingChangesUntilTheCodeIsConfirmed() throws Exception {
		String token = accessTokenFor(OLD_EMAIL);
		requestChangeAndCaptureCode(token);

		mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.email").value(OLD_EMAIL))
				.andExpect(jsonPath("$.emailVerified").value(true));
	}

	/**
	 * The consequence of sending the code only to the old address: it proves the
	 * account, and nothing at all about the address being moved to.
	 */
	@Test
	void confirmingMovesTheAddressAndLeavesItUnverified() throws Exception {
		String token = accessTokenFor(OLD_EMAIL);
		String code = requestChangeAndCaptureCode(token);

		mockMvc.perform(post("/api/v1/users/me/confirm-email-change")
						.header("Authorization", "Bearer " + token)
						.contentType("application/json")
						.content("{\"code\":\"%s\"}".formatted(code)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(NEW_EMAIL))
				.andExpect(jsonPath("$.emailVerified").value(false));
	}

	@Test
	void theWholeFlowEndsWithTheNewAddressVerifiedAndAbleToLogIn() throws Exception {
		String token = accessTokenFor(OLD_EMAIL);
		String changeCode = requestChangeAndCaptureCode(token);

		mockMvc.perform(post("/api/v1/users/me/confirm-email-change")
						.header("Authorization", "Bearer " + token)
						.contentType("application/json")
						.content("{\"code\":\"%s\"}".formatted(changeCode)))
				.andExpect(status().isOk());

		// Confirmation issues a fresh verification code, to the new address.
		ArgumentCaptor<String> verificationCode = ArgumentCaptor.forClass(String.class);
		verify(emailProvider).sendVerificationCode(eq(NEW_EMAIL), verificationCode.capture(), any());

		mockMvc.perform(post("/api/v1/auth/verify-email")
						.contentType("application/json")
						.content("{\"email\":\"%s\",\"code\":\"%s\"}"
								.formatted(NEW_EMAIL, verificationCode.getValue())))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType("application/json")
						.content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(NEW_EMAIL, PASSWORD)))
				.andExpect(status().isOk());
	}

	/**
	 * The recovery path, asserted rather than assumed. A mistyped address leaves
	 * the account unverified and therefore unable to log in, so the session that
	 * made the change is the only way to correct it.
	 */
	@Test
	void theSessionSurvivesTheChangeSoAMistakeCanStillBeCorrected() throws Exception {
		String refreshToken = refreshTokenFor(OLD_EMAIL);
		String token = accessTokenFor(OLD_EMAIL);
		String code = requestChangeAndCaptureCode(token);

		mockMvc.perform(post("/api/v1/users/me/confirm-email-change")
						.header("Authorization", "Bearer " + token)
						.contentType("application/json")
						.content("{\"code\":\"%s\"}".formatted(code)))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType("application/json")
						.content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)))
				.andExpect(status().isOk());
	}

	@Test
	void aWrongPasswordIsRefusedAndNoEmailIsSent() throws Exception {
		mockMvc.perform(post("/api/v1/users/me/change-email")
						.header("Authorization", "Bearer " + accessTokenFor(OLD_EMAIL))
						.contentType("application/json")
						.content("{\"newEmail\":\"%s\",\"currentPassword\":\"not-the-right-one\"}"
								.formatted(NEW_EMAIL)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"));

		verify(emailProvider, never()).sendEmailChangeCode(any(), any(), any(), any());
	}

	@Test
	void anAddressSomebodyElseAlreadyHoldsIsRefused() throws Exception {
		account(NEW_EMAIL);

		mockMvc.perform(post("/api/v1/users/me/change-email")
						.header("Authorization", "Bearer " + accessTokenFor(OLD_EMAIL))
						.contentType("application/json")
						.content("{\"newEmail\":\"%s\",\"currentPassword\":\"%s\"}"
								.formatted(NEW_EMAIL, PASSWORD)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
	}

	/** A code issued for one purpose must never open another. */
	@Test
	void averificationCodeCannotConfirmAnEmailChange() throws Exception {
		String token = accessTokenFor(OLD_EMAIL);
		requestChangeAndCaptureCode(token);

		mockMvc.perform(post("/api/v1/auth/resend-verification")
						.contentType("application/json")
						.content("{\"email\":\"%s\"}".formatted(OLD_EMAIL)))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/v1/users/me/confirm-email-change")
						.header("Authorization", "Bearer " + token)
						.contentType("application/json")
						.content("{\"code\":\"000000\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VERIFICATION_CODE_INVALID"));

		assertThat(userRepository.findByEmail(OLD_EMAIL)).isPresent();
	}

	@Test
	void bothEndpointsRefuseACallerWithNoToken() throws Exception {
		mockMvc.perform(post("/api/v1/users/me/change-email")
						.contentType("application/json")
						.content("{\"newEmail\":\"%s\",\"currentPassword\":\"%s\"}"
								.formatted(NEW_EMAIL, PASSWORD)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

		mockMvc.perform(post("/api/v1/users/me/confirm-email-change")
						.contentType("application/json")
						.content("{\"code\":\"123456\"}"))
				.andExpect(status().isUnauthorized());
	}
}