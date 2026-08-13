package ro.garajulmeu.auth;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import ro.garajulmeu.TestcontainersConfiguration;
import ro.garajulmeu.user.User;
import ro.garajulmeu.user.UserRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
		String body = mockMvc.perform(post("/api/v1/auth/login")
						.contentType("application/json")
						.content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		return JsonPath.read(body, "$.accessToken");
	}

	@Test
	void aTokenFromLoginOpensAProtectedRoute() throws Exception {
		String token = login(EMAIL, PASSWORD);

		mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(EMAIL))
				.andExpect(jsonPath("$.emailVerified").value(true));
	}

	@Test
	void theProtectedRouteRefusesARequestWithNoToken() throws Exception {
		mockMvc.perform(get("/api/v1/users/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void theProtectedRouteRefusesAForgedToken() throws Exception {
		mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer not-a-real-token"))
				.andExpect(status().isUnauthorized());
	}

	/** The profile must never carry the password hash, whatever else changes. */
	@Test
	void theProfileNeverExposesThePasswordHash() throws Exception {
		String token = login(EMAIL, PASSWORD);

		String body = mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
				.andReturn().getResponse().getContentAsString();

		org.assertj.core.api.Assertions.assertThat(body)
				.doesNotContain("argon2")
				.doesNotContain("passwordHash");
	}

	@Test
	void loginWithAWrongPasswordAnswersInvalidCredentials() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType("application/json")
						.content("{\"email\":\"%s\",\"password\":\"completely-wrong-password\"}".formatted(EMAIL)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
	}

	/** An unknown address must be indistinguishable from a wrong password. */
	@Test
	void loginWithAnUnknownAddressAnswersTheSameCode() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType("application/json")
						.content("{\"email\":\"nobody@example.com\",\"password\":\"%s\"}".formatted(PASSWORD)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
	}

	@Test
	void loginRefusesAnUnverifiedAccount() throws Exception {
		User unverified = new User("Marius Robert", "unverified@example.com", passwordEncoder.encode(PASSWORD));
		userRepository.saveAndFlush(unverified);

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType("application/json")
						.content("{\"email\":\"unverified@example.com\",\"password\":\"%s\"}".formatted(PASSWORD)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
	}
}