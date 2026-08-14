package ro.garajulmeu.user;

import java.time.Instant;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The annotations match AuthFlowTest exactly, including the unused EmailProvider
 * mock. That is deliberate: an identical context configuration reuses the cached
 * context instead of starting a fifth PostgreSQL container. Changing any one of
 * them costs about three seconds on every build from then on.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class UserProfileFlowTest {

	private static final String EMAIL = "profile@example.com";

	private static final String PASSWORD = "the-original-long-password";

	private static final String NEW_PASSWORD = "a-replacement-long-password";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	/** Unused. Present only so this class shares AuthFlowTest's context. */
	@MockitoBean
	private EmailProvider emailProvider;

	@BeforeEach
	void givenAVerifiedAccount() {
		account(EMAIL);
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

	/** The difference between PATCH and PUT: absent means unchanged. */
	@Test
	void patchLeavesTheFieldsTheBodyDoesNotMention() throws Exception {
		String token = accessTokenFor(EMAIL);

		mockMvc.perform(patch("/api/v1/users/me")
						.header("Authorization", "Bearer " + token)
						.contentType("application/json")
						.content("{\"fullName\":\"  Marius R.  \"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fullName").value("Marius R."))
				.andExpect(jsonPath("$.preferredLanguage").value("ro"))
				.andExpect(jsonPath("$.timezone").value("Europe/Bucharest"));
	}

	@Test
	void patchAcceptsALanguageAndARealTimezone() throws Exception {
		String token = accessTokenFor(EMAIL);

		mockMvc.perform(patch("/api/v1/users/me")
						.header("Authorization", "Bearer " + token)
						.contentType("application/json")
						.content("{\"preferredLanguage\":\"en\",\"timezone\":\"Europe/London\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.preferredLanguage").value("en"))
				.andExpect(jsonPath("$.timezone").value("Europe/London"));
	}

	@Test
	void patchRejectsAWhitespaceOnlyName() throws Exception {
		String token = accessTokenFor(EMAIL);

		mockMvc.perform(patch("/api/v1/users/me")
						.header("Authorization", "Bearer " + token)
						.contentType("application/json")
						.content("{\"fullName\":\"   \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("fullName"));
	}

	/** A plausible-looking string is not a zone. Phase 11 computes day boundaries from this. */
	@Test
	void patchRejectsATimezoneThatDoesNotExist() throws Exception {
		String token = accessTokenFor(EMAIL);

		mockMvc.perform(patch("/api/v1/users/me")
						.header("Authorization", "Bearer " + token)
						.contentType("application/json")
						.content("{\"timezone\":\"Europe/Bucuresti\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	/**
	 * The ownership property in the shape this endpoint has: there is no path
	 * parameter to tamper with, so what must be proved is that the token alone
	 * decides whose row changes.
	 */
	@Test
	void patchChangesOnlyTheAccountTheTokenBelongsTo() throws Exception {
		User other = account("someone-else@example.com");
		String token = accessTokenFor(EMAIL);

		mockMvc.perform(patch("/api/v1/users/me")
						.header("Authorization", "Bearer " + token)
						.contentType("application/json")
						.content("{\"fullName\":\"Renamed\"}"))
				.andExpect(status().isOk());

		assertThat(userRepository.findById(other.getId()).orElseThrow().getFullName())
				.isEqualTo("Marius Robert");
	}

	@Test
	void changePasswordRefusesAWrongCurrentPassword() throws Exception {
		String token = accessTokenFor(EMAIL);

		mockMvc.perform(post("/api/v1/users/me/change-password")
						.header("Authorization", "Bearer " + token)
						.contentType("application/json")
						.content("{\"currentPassword\":\"not-the-right-one\",\"newPassword\":\"%s\"}"
								.formatted(NEW_PASSWORD)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"));

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType("application/json")
						.content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(EMAIL, PASSWORD)))
				.andExpect(status().isOk());
	}

	@Test
	void changingThePasswordEndsEverySessionIncludingTheCallersOwn() throws Exception {
		String refreshToken = refreshTokenFor(EMAIL);
		String token = accessTokenFor(EMAIL);

		mockMvc.perform(post("/api/v1/users/me/change-password")
						.header("Authorization", "Bearer " + token)
						.contentType("application/json")
						.content("{\"currentPassword\":\"%s\",\"newPassword\":\"%s\"}"
								.formatted(PASSWORD, NEW_PASSWORD)))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType("application/json")
						.content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType("application/json")
						.content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(EMAIL, NEW_PASSWORD)))
				.andExpect(status().isOk());
	}

	@Test
	void bothEndpointsRefuseACallerWithNoToken() throws Exception {
		mockMvc.perform(patch("/api/v1/users/me")
						.contentType("application/json")
						.content("{\"fullName\":\"Anyone\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

		mockMvc.perform(post("/api/v1/users/me/change-password")
						.contentType("application/json")
						.content("{\"currentPassword\":\"x\",\"newPassword\":\"%s\"}".formatted(NEW_PASSWORD)))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/api/v1/users/me"))
				.andExpect(status().isUnauthorized());
	}
}