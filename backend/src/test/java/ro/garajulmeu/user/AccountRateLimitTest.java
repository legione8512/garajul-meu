package ro.garajulmeu.user;

import java.time.Instant;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import ro.garajulmeu.TestcontainersConfiguration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The annotations and the four properties match AuthRateLimitTest character for
 * character, so this class reuses that cached context instead of starting a
 * sixth PostgreSQL container. Reordering the properties or adding one would be
 * enough to lose the reuse, because they form part of the context cache key.
 *
 * <p>No {@code @Transactional}, for the same reason as there: the limiter is a
 * singleton and its counters are not transactional, so rolling the database back
 * would not roll them back and a test would be lying about its starting state.
 * Each test therefore uses its own account and its own addresses, and the
 * accounts it creates simply stay in a container that is about to be discarded.
 *
 * <p>Every attempt sends a <em>wrong</em> current password on purpose. The limit
 * is checked before the service, so a refused attempt still spends budget, while
 * a successful password change would revoke every session and rewrite the hash -
 * making the second attempt a different test from the first.
 */
@SpringBootTest(properties = {
		"garajul-meu.rate-limit.credential-check.limit=2",
		"garajul-meu.rate-limit.credential-check.window=15m",
		"garajul-meu.rate-limit.email-dispatch.limit=2",
		"garajul-meu.rate-limit.email-dispatch.window=1h" })
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AccountRateLimitTest {

	private static final String PASSWORD = "the-original-long-password";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private static RequestPostProcessor from(String address) {
		return request -> {
			request.setRemoteAddr(address);
			return request;
		};
	}

	/**
	 * Creates a verified account and signs in once. The address matters: logging
	 * in spends the auth limiter's address budget, which is also two here, so a
	 * test that signed in three times from one address would fail on the login
	 * rather than on the thing it means to assert.
	 */
	private String tokenFor(String email, String address) throws Exception {
		User user = new User("Marius Robert", email, passwordEncoder.encode(PASSWORD));
		user.setEmailVerifiedAt(Instant.now());
		userRepository.saveAndFlush(user);

		String body = mockMvc.perform(post("/api/v1/auth/login")
						.with(from(address))
						.contentType("application/json")
						.content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		return JsonPath.read(body, "$.accessToken");
	}

	private void changePassword(String token, int expectedStatus) throws Exception {
		mockMvc.perform(post("/api/v1/users/me/change-password")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType("application/json")
						.content("{\"currentPassword\":\"not-the-right-password\","
								+ "\"newPassword\":\"a-replacement-long-password\"}"))
				.andExpect(status().is(expectedStatus));
	}

	/** A valid token stops being a licence to grind once the budget is gone. */
	@Test
	void aValidTokenDoesNotBuyUnlimitedPasswordAttempts() throws Exception {
		String token = tokenFor("limit-password@example.com", "10.1.0.1");

		changePassword(token, 400);
		changePassword(token, 400);

		mockMvc.perform(post("/api/v1/users/me/change-password")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType("application/json")
						.content("{\"currentPassword\":\"not-the-right-password\","
								+ "\"newPassword\":\"a-replacement-long-password\"}"))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.code").value("RATE_LIMITED"));
	}

	/**
	 * Both accounts sign in from one address - exactly filling that address's
	 * login budget and no more - so the only thing that can separate them is the
	 * account key. If the budget were keyed on the address, the quiet account
	 * would be refused too.
	 */
	@Test
	void theBudgetBelongsToTheAccountAndNotToTheAddress() throws Exception {
		String noisy = tokenFor("limit-noisy@example.com", "10.1.1.1");
		String quiet = tokenFor("limit-quiet@example.com", "10.1.1.1");

		changePassword(noisy, 400);
		changePassword(noisy, 400);
		changePassword(noisy, 429);

		changePassword(quiet, 400);
	}

	/** Different policies must not draw on one another's budget. */
	@Test
	void spendingThePasswordBudgetLeavesTheEmailBudgetIntact() throws Exception {
		String token = tokenFor("limit-separate@example.com", "10.1.2.1");

		changePassword(token, 400);
		changePassword(token, 400);
		changePassword(token, 429);

		// Refused for the wrong password, not for the budget - which is the
		// distinction being asserted. 429 here would mean one counter served two
		// policies.
		mockMvc.perform(post("/api/v1/users/me/change-email")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType("application/json")
						.content("{\"newEmail\":\"moved@example.com\","
								+ "\"currentPassword\":\"not-the-right-password\"}"))
				.andExpect(status().isBadRequest());
	}

	/**
	 * The boundary, asserted rather than assumed. Reading and editing the profile
	 * are deliberately unlimited: neither computes a hash nor sends anything, and
	 * throttling them would punish a client that polls while protecting nothing.
	 * If somebody later limits everything under {@code /users/me} for tidiness,
	 * this test is what tells them it was a decision and not an oversight.
	 */
	@Test
	void readingAndEditingTheProfileStayOpenWhenTheHashBudgetIsGone() throws Exception {
		String token = tokenFor("limit-reads@example.com", "10.1.3.1");

		changePassword(token, 400);
		changePassword(token, 400);
		changePassword(token, 429);

		mockMvc.perform(get("/api/v1/users/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk());

		mockMvc.perform(patch("/api/v1/users/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType("application/json")
						.content("{\"fullName\":\"Marius Robert\"}"))
				.andExpect(status().isOk());
	}
}