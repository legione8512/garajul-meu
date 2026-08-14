package ro.garajulmeu.user;

import java.time.Instant;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;

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
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Annotations identical to AuthFlowTest, including the unused EmailProvider mock,
 * so the cached context is reused and no further container starts.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class AccountDeletionTest {

	private static final String EMAIL = "leaving@example.com";

	private static final String PASSWORD = "the-original-long-password";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private EntityManager entityManager;

	/** Unused. Present only so this class shares AuthFlowTest's context. */
	@MockitoBean
	private EmailProvider emailProvider;

	private UUID accountId;

	@BeforeEach
	void givenAVerifiedAccount() {
		accountId = account(EMAIL).getId();
	}

	private User account(String email) {
		User user = new User("Marius Robert", email, passwordEncoder.encode(PASSWORD));
		user.setEmailVerifiedAt(Instant.now());
		return userRepository.saveAndFlush(user);
	}

	private String accessTokenFor(String email) throws Exception {
		String body = login(email);
		return JsonPath.read(body, "$.accessToken");
	}

	private String refreshTokenFor(String email) throws Exception {
		return JsonPath.read(login(email), "$.refreshToken");
	}

	private String login(String email) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/login")
						.contentType("application/json")
						.content("{\"email\":\"%s\",\"password\":\"%s\",\"refreshTokenInBody\":true}"
								.formatted(email, PASSWORD)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
	}

	private void deleteWith(String token, String password) throws Exception {
		mockMvc.perform(delete("/api/v1/users/me")
						.header("Authorization", "Bearer " + token)
						.contentType("application/json")
						.content("{\"currentPassword\":\"%s\"}".formatted(password)))
				.andExpect(status().isNoContent());
	}

	/** Counted in the database, because the foreign key is what deletes these. */
	private long verificationTokensOf(UUID userId) {
		return entityManager
				.createQuery("select count(t) from VerificationToken t where t.userId = :id", Long.class)
				.setParameter("id", userId)
				.getSingleResult();
	}

	private long refreshTokensOf(UUID userId) {
		return entityManager
				.createQuery("select count(t) from RefreshToken t where t.userId = :id", Long.class)
				.setParameter("id", userId)
				.getSingleResult();
	}

	@Test
	void deletingRemovesTheAccountItself() throws Exception {
		deleteWith(accessTokenFor(EMAIL), PASSWORD);

		assertThat(userRepository.findById(accountId)).isEmpty();
	}

	/**
	 * The cascade, asserted rather than trusted. Neither child entity is mapped as
	 * an association, so nothing in Java removes these rows - only the foreign key
	 * does. A migration that dropped it would leave orphans in silence.
	 */
	@Test
	void deletingRemovesEveryCodeAndEverySessionThatHungOffTheAccount() throws Exception {
		String token = accessTokenFor(EMAIL);

		mockMvc.perform(post("/api/v1/auth/forgot-password")
						.contentType("application/json")
						.content("{\"email\":\"%s\"}".formatted(EMAIL)))
				.andExpect(status().isNoContent());

		assertThat(verificationTokensOf(accountId)).isPositive();
		assertThat(refreshTokensOf(accountId)).isPositive();

		deleteWith(token, PASSWORD);

		assertThat(verificationTokensOf(accountId)).isZero();
		assertThat(refreshTokensOf(accountId)).isZero();
	}

	/**
	 * What section 24 means by permanent. A soft delete or an anonymised tombstone
	 * row would keep the unique index occupied and this registration would answer
	 * EMAIL_ALREADY_EXISTS - so this test is the one that would catch the day
	 * somebody decides deletion should merely hide the account.
	 */
	@Test
	void theAddressIsFreeForANewAccountAfterwards() throws Exception {
		deleteWith(accessTokenFor(EMAIL), PASSWORD);

		mockMvc.perform(post("/api/v1/auth/register")
						.contentType("application/json")
						.content("""
								{"fullName":"Somebody Else","email":"%s",
								 "password":"another-long-enough-password","preferredLanguage":"ro"}"""
								.formatted(EMAIL)))
				.andExpect(status().isCreated());
	}

	@Test
	void aWrongPasswordDeletesNothing() throws Exception {
		mockMvc.perform(delete("/api/v1/users/me")
						.header("Authorization", "Bearer " + accessTokenFor(EMAIL))
						.contentType("application/json")
						.content("{\"currentPassword\":\"not-the-right-one\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"));

		assertThat(userRepository.findById(accountId)).isPresent();
	}

	@Test
	void theRefreshTokenStopsWorkingImmediately() throws Exception {
		String refreshToken = refreshTokenFor(EMAIL);

		deleteWith(accessTokenFor(EMAIL), PASSWORD);

		mockMvc.perform(post("/api/v1/auth/refresh")
						.contentType("application/json")
						.content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)))
				.andExpect(status().isUnauthorized());
	}

	/**
	 * A stateless JWT cannot be recalled, so it stays cryptographically valid for
	 * the rest of its short life. It opens nothing: every route resolves the
	 * account first and finds none. Asserted because it is a real consequence of
	 * the design, and someone reading the code later deserves to find it decided
	 * rather than discovered.
	 */
	@Test
	void theAccessTokenOutlivesTheAccountButOpensNothing() throws Exception {
		String token = accessTokenFor(EMAIL);

		deleteWith(token, PASSWORD);

		mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
	}

	@Test
	void theResponseClearsTheRefreshCookie() throws Exception {
		mockMvc.perform(delete("/api/v1/users/me")
						.header("Authorization", "Bearer " + accessTokenFor(EMAIL))
						.contentType("application/json")
						.content("{\"currentPassword\":\"%s\"}".formatted(PASSWORD)))
				.andExpect(status().isNoContent())
				.andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
	}

	@Test
	void deletingOneAccountLeavesAnotherIntact() throws Exception {
		User other = account("stays@example.com");

		deleteWith(accessTokenFor(EMAIL), PASSWORD);

		assertThat(userRepository.findById(other.getId())).isPresent();
	}

	@Test
	void refusesACallerWithNoToken() throws Exception {
		mockMvc.perform(delete("/api/v1/users/me")
						.contentType("application/json")
						.content("{\"currentPassword\":\"%s\"}".formatted(PASSWORD)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

		assertThat(userRepository.findById(accountId)).isPresent();
	}
}