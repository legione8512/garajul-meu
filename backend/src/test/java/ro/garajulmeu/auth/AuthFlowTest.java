package ro.garajulmeu.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

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

import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;
import ro.garajulmeu.TestcontainersConfiguration;
import ro.garajulmeu.common.RequestIdFilter;
import ro.garajulmeu.email.EmailProvider;
import ro.garajulmeu.user.User;
import ro.garajulmeu.user.UserRepository;

/**
 * Exercises the real HTTP surface: log in, receive a token, use it on a
 * protected route. Nothing here reaches into the service layer.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthFlowTest {

	private static final String EMAIL = "flow@example.com";

	private static final String PASSWORD = "a-sufficiently-long-password";

	private static final String NEW_PASSWORD = "a-replacement-long-password";

	/** Replaces the logging provider so the emailed reset code can be read. */
	@MockitoBean
	private EmailProvider emailProvider;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void givenAVerifiedAccount() {
		User user = new User("Marius Robert", EMAIL, passwordEncoder.encode(PASSWORD));
		user.setEmailVerifiedAt(Instant.now());
		userRepository.saveAndFlush(user);
	}

	private String login(String email, String password) throws Exception {
		String body = mockMvc
				.perform(post("/api/v1/auth/login").contentType("application/json")
						.content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

		return JsonPath.read(body, "$.accessToken");
	}

	@Test
	void aTokenFromLoginOpensAProtectedRoute() throws Exception {
		String token = login(EMAIL, PASSWORD);

		mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token)).andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(EMAIL)).andExpect(jsonPath("$.emailVerified").value(true));
	}

	@Test
	void theProtectedRouteRefusesARequestWithNoToken() throws Exception {
		mockMvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
	}

	@Test
	void theProtectedRouteRefusesAForgedToken() throws Exception {
		mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer not-a-real-token"))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
	}

	/**
	 * The actual defect being fixed: an authentication failure used to answer with
	 * an empty body, outside the correlation mechanism every other error uses. If
	 * someone later rebuilds this response without the MDC lookup, this fails.
	 */
	@Test
	void theUnauthenticatedErrorJoinsTheCorrelationMechanism() throws Exception {
		var result = mockMvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.path").value("/api/v1/users/me")).andExpect(jsonPath("$.requestId").isNotEmpty())
				.andReturn();

		assertThat(JsonPath.<String>read(result.getResponse().getContentAsString(), "$.requestId"))
				.isEqualTo(result.getResponse().getHeader(RequestIdFilter.HEADER));
	}

	/** The profile must never carry the password hash, whatever else changes. */
	@Test
	void theProfileNeverExposesThePasswordHash() throws Exception {
		String token = login(EMAIL, PASSWORD);

		String body = mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token)).andReturn()
				.getResponse().getContentAsString();

		org.assertj.core.api.Assertions.assertThat(body).doesNotContain("argon2").doesNotContain("passwordHash");
	}

	@Test
	void loginWithAWrongPasswordAnswersInvalidCredentials() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login").contentType("application/json")
				.content("{\"email\":\"%s\",\"password\":\"completely-wrong-password\"}".formatted(EMAIL)))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
	}

	/** An unknown address must be indistinguishable from a wrong password. */
	@Test
	void loginWithAnUnknownAddressAnswersTheSameCode() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login").contentType("application/json")
				.content("{\"email\":\"nobody@example.com\",\"password\":\"%s\"}".formatted(PASSWORD)))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
	}

	@Test
	void loginRefusesAnUnverifiedAccount() throws Exception {
		User unverified = new User("Marius Robert", "unverified@example.com", passwordEncoder.encode(PASSWORD));
		userRepository.saveAndFlush(unverified);

		mockMvc.perform(post("/api/v1/auth/login").contentType("application/json")
				.content("{\"email\":\"unverified@example.com\",\"password\":\"%s\"}".formatted(PASSWORD)))
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
	}

	private MvcResultHolder loginRaw(boolean tokenInBody) throws Exception {
		var result = mockMvc
				.perform(
						post("/api/v1/auth/login").contentType("application/json")
								.content("{\"email\":\"%s\",\"password\":\"%s\",\"refreshTokenInBody\":%s}"
										.formatted(EMAIL, PASSWORD, tokenInBody)))
				.andExpect(status().isOk()).andReturn();

		Cookie refreshCookie = result.getResponse().getCookie(RefreshCookies.NAME);
		return new MvcResultHolder(result.getResponse().getContentAsString(),
				refreshCookie == null ? null : refreshCookie.getValue());
	}

	private record MvcResultHolder(String body, String cookieValue) {
	}

	@Test
	void loginAlwaysSetsAnHttpOnlyRefreshCookie() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login").contentType("application/json")
				.content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(EMAIL, PASSWORD)))
				.andExpect(status().isOk()).andExpect(cookie().exists(RefreshCookies.NAME))
				.andExpect(cookie().httpOnly(RefreshCookies.NAME, true))
				.andExpect(cookie().secure(RefreshCookies.NAME, true))
				.andExpect(jsonPath("$.refreshToken").doesNotExist());
	}

	@Test
	void aNativeClientReceivesTheRefreshTokenInTheBody() throws Exception {
		MvcResultHolder login = loginRaw(true);

		assertThat(JsonPath.<String>read(login.body(), "$.refreshToken")).isNotBlank();
	}

	@Test
	void refreshOnTheCookieChannelAnswersOnTheCookieChannel() throws Exception {
		MvcResultHolder login = loginRaw(false);

		mockMvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie(RefreshCookies.NAME, login.cookieValue())))
				.andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.refreshToken").doesNotExist()).andExpect(cookie().exists(RefreshCookies.NAME));
	}

	@Test
	void refreshOnTheBodyChannelAnswersOnTheBodyChannel() throws Exception {
		String first = JsonPath.read(loginRaw(true).body(), "$.refreshToken");

		String body = mockMvc
				.perform(post("/api/v1/auth/refresh").contentType("application/json")
						.content("{\"refreshToken\":\"%s\"}".formatted(first)))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

		assertThat(JsonPath.<String>read(body, "$.refreshToken")).isNotBlank().isNotEqualTo(first);
	}

	/**
	 * The mechanism that turns a stolen token into an alarm.
	 *
	 * <p>
	 * The replacement is collected and used before the replay, and that is the
	 * whole shape of this test. Replaying the instant after a rotation is now the
	 * forgiven case - a response that never arrived rather than a second holder -
	 * so stopping at one refresh would assert the grace window while claiming to
	 * assert the alarm. RefreshTokenService carries the reasoning.
	 */
	@Test
	void replayingASpentRefreshTokenIsRejected() throws Exception {
		String first = JsonPath.read(loginRaw(true).body(), "$.refreshToken");

		String secondBody = mockMvc
				.perform(post("/api/v1/auth/refresh").contentType("application/json")
						.content("{\"refreshToken\":\"%s\"}".formatted(first)))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

		String second = JsonPath.read(secondBody, "$.refreshToken");

		mockMvc.perform(post("/api/v1/auth/refresh").contentType("application/json")
				.content("{\"refreshToken\":\"%s\"}".formatted(second))).andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/auth/refresh").contentType("application/json")
				.content("{\"refreshToken\":\"%s\"}".formatted(first))).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("REFRESH_TOKEN_REUSED"));
	}

	/**
	 * The accident the window exists for, asserted at the edge a real client meets
	 * rather than only in the service: the rotation happened, the answer never
	 * arrived, and the same token is presented again.
	 */
	@Test
	void aReplayWhoseReplacementWasNeverCollectedIsAnsweredWithAFreshToken() throws Exception {
		String first = JsonPath.read(loginRaw(true).body(), "$.refreshToken");

		String lostBody = mockMvc
				.perform(post("/api/v1/auth/refresh").contentType("application/json")
						.content("{\"refreshToken\":\"%s\"}".formatted(first)))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

		String neverCollected = JsonPath.read(lostBody, "$.refreshToken");

		String body = mockMvc
				.perform(post("/api/v1/auth/refresh").contentType("application/json")
						.content("{\"refreshToken\":\"%s\"}".formatted(first)))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

		assertThat(JsonPath.<String>read(body, "$.refreshToken")).isNotBlank().isNotEqualTo(first)
				.isNotEqualTo(neverCollected);
	}

	@Test
	void refreshWithNoTokenAtAllIsRejected() throws Exception {
		mockMvc.perform(post("/api/v1/auth/refresh")).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("REFRESH_TOKEN_INVALID"));
	}

	@Test
	void logoutClearsTheCookieAndEndsTheSession() throws Exception {
		MvcResultHolder login = loginRaw(false);

		mockMvc.perform(post("/api/v1/auth/logout").cookie(new Cookie(RefreshCookies.NAME, login.cookieValue())))
				.andExpect(status().isNoContent()).andExpect(cookie().maxAge(RefreshCookies.NAME, 0));

		mockMvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie(RefreshCookies.NAME, login.cookieValue())))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void loggingOutWithoutATokenIsNotAnError() throws Exception {
		mockMvc.perform(post("/api/v1/auth/logout")).andExpect(status().isNoContent());
	}

	/**
	 * Specification section 14: the answer must not reveal who holds an account.
	 */
	@Test
	void forgotPasswordAnswersIdenticallyWhetherOrNotTheAddressExists() throws Exception {
		mockMvc.perform(post("/api/v1/auth/forgot-password").contentType("application/json")
				.content("{\"email\":\"%s\"}".formatted(EMAIL))).andExpect(status().isNoContent());

		mockMvc.perform(post("/api/v1/auth/forgot-password").contentType("application/json")
				.content("{\"email\":\"nobody@example.com\"}")).andExpect(status().isNoContent());
	}

	/** The flow a real person performs, end to end over HTTP. */
	@Test
	void theWholeResetFlowEndsWithTheNewPasswordWorking() throws Exception {
		mockMvc.perform(post("/api/v1/auth/forgot-password").contentType("application/json")
				.content("{\"email\":\"%s\"}".formatted(EMAIL))).andExpect(status().isNoContent());

		ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
		verify(emailProvider).sendPasswordResetCode(eq(EMAIL), code.capture(), any());

		mockMvc.perform(post("/api/v1/auth/reset-password").contentType("application/json")
				.content("{\"email\":\"%s\",\"code\":\"%s\",\"newPassword\":\"%s\"}".formatted(EMAIL, code.getValue(),
						NEW_PASSWORD)))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/v1/auth/login").contentType("application/json")
				.content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(EMAIL, NEW_PASSWORD)))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/auth/login").contentType("application/json")
				.content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(EMAIL, PASSWORD)))
				.andExpect(status().isUnauthorized());
	}

	/**
	 * Six digits or nothing, checked before the request can cost an Argon2 hash.
	 */
	@Test
	void resetPasswordRejectsAMalformedCodeBeforeItCostsAHash() throws Exception {
		mockMvc.perform(post("/api/v1/auth/reset-password").contentType("application/json")
				.content("{\"email\":\"%s\",\"code\":\"12\",\"newPassword\":\"%s\"}".formatted(EMAIL, NEW_PASSWORD)))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("code"));
	}

	/** Section 14 again: a reset must not leave the old sessions alive. */
	@Test
	void resettingEndsASessionThatWasAlreadyOpen() throws Exception {
		String refreshToken = JsonPath.read(loginRaw(true).body(), "$.refreshToken");

		mockMvc.perform(post("/api/v1/auth/forgot-password").contentType("application/json")
				.content("{\"email\":\"%s\"}".formatted(EMAIL))).andExpect(status().isNoContent());

		ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
		verify(emailProvider).sendPasswordResetCode(eq(EMAIL), code.capture(), any());

		mockMvc.perform(post("/api/v1/auth/reset-password").contentType("application/json")
				.content("{\"email\":\"%s\",\"code\":\"%s\",\"newPassword\":\"%s\"}".formatted(EMAIL, code.getValue(),
						NEW_PASSWORD)))
				.andExpect(status().isNoContent());

		// Only the status is asserted. A token revoked by reset is indistinguishable
		// from one revoked by rotation, so this answers REFRESH_TOKEN_REUSED rather
		// than something more precise - an observation already recorded in
		// PROJECT_STATE, not a defect, and not worth pinning down in a test.
		mockMvc.perform(post("/api/v1/auth/refresh").contentType("application/json")
				.content("{\"refreshToken\":\"%s\"}".formatted(refreshToken))).andExpect(status().isUnauthorized());
	}
}