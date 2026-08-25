package ro.garajulmeu.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import ro.garajulmeu.TestcontainersConfiguration;

/**
 * Two attempts per policy, so the boundary is reached in three requests instead
 * of eleven. Both the window and the limit have to be given: a policy with a
 * limit and no window is refused at startup rather than binding a null.
 *
 * <p>
 * Every test uses its own address range and its own addresses, because the
 * limiter is a singleton whose counters outlive a test method. No account is
 * created: an unknown address answers INVALID_CREDENTIALS, and the only thing
 * being asserted is 401 against 429.
 */
@SpringBootTest(properties = { "garajul-meu.rate-limit.credential-check.limit=2",
		"garajul-meu.rate-limit.credential-check.window=15m", "garajul-meu.rate-limit.email-dispatch.limit=2",
		"garajul-meu.rate-limit.email-dispatch.window=1h" })
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthRateLimitTest {

	/**
	 * One more than the sixty-per-hour that {@code /auth/refresh} carried until
	 * 2026-08-25. Named rather than inlined, because the number is the argument:
	 * anything at or below the old limit would pass whether or not the limit is
	 * still there.
	 */
	private static final int MORE_THAN_THE_REMOVED_LIMIT = 61;

	@Autowired
	private MockMvc mockMvc;

	private static RequestPostProcessor from(String address) {
		return request -> {
			request.setRemoteAddr(address);
			return request;
		};
	}

	private void login(String address, String email, int expectedStatus) throws Exception {
		mockMvc.perform(post("/api/v1/auth/login").with(from(address)).contentType("application/json")
				.content("{\"email\":\"%s\",\"password\":\"a-sufficiently-long-password\"}".formatted(email)))
				.andExpect(status().is(expectedStatus));
	}

	/** A fresh email each time, so only the address budget can be what runs out. */
	@Test
	void oneAddressCannotKeepGuessingByChangingTheEmail() throws Exception {
		login("10.0.0.1", "first@example.com", 401);
		login("10.0.0.1", "second@example.com", 401);

		mockMvc.perform(post("/api/v1/auth/login").with(from("10.0.0.1")).contentType("application/json")
				.content("{\"email\":\"third@example.com\",\"password\":\"a-sufficiently-long-password\"}"))
				.andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.code").value("RATE_LIMITED"));
	}

	/** One noisy address must not lock out everybody else. */
	@Test
	void anotherAddressIsUnaffected() throws Exception {
		login("10.0.1.1", "a@example.com", 401);
		login("10.0.1.1", "b@example.com", 401);
		login("10.0.1.1", "c@example.com", 429);

		login("10.0.1.2", "d@example.com", 401);
	}

	/** A fresh address each time, so only the email budget can be what runs out. */
	@Test
	void oneAccountCannotBeAttackedFromManyAddresses() throws Exception {
		login("10.0.2.1", "target@example.com", 401);
		login("10.0.2.2", "target@example.com", 401);

		login("10.0.2.3", "target@example.com", 429);
	}

	/** Different policies must not draw on one another's budget. */
	@Test
	void spendingTheLoginBudgetLeavesTheResendBudgetIntact() throws Exception {
		login("10.0.3.1", "x@example.com", 401);
		login("10.0.3.1", "y@example.com", 401);
		login("10.0.3.1", "z@example.com", 429);

		mockMvc.perform(post("/api/v1/auth/resend-verification").with(from("10.0.3.1")).contentType("application/json")
				.content("{\"email\":\"someone@example.com\"}")).andExpect(status().isNoContent());
	}

	/**
	 * The decision of 2026-08-25, asserted as a decision rather than left as an
	 * absence somebody later reads as an oversight.
	 *
	 * <p>
	 * Rotation was limited to sixty an hour by network address, which is the only
	 * key available before the token has been looked up. A mobile application makes
	 * that untenable: a carrier's users leave through a handful of CGNAT addresses,
	 * every launch refreshes, and a ten-minute access token means about six an hour
	 * each - so ten people exhaust sixty and the eleventh is refused while
	 * restoring a session, which reads as a broken application.
	 *
	 * <p>
	 * Every attempt answers 401 because the token is nonsense. <strong>401 rather
	 * than 429 is the entire assertion</strong>, and the count is what gives it
	 * teeth: this fails the day an address key is put back on this endpoint, rather
	 * than the day somebody notices in production.
	 */
	@Test
	void rotationIsNeverRefusedByAddressHoweverOftenItIsAsked() throws Exception {
		for (int attempt = 1; attempt <= MORE_THAN_THE_REMOVED_LIMIT; attempt++) {
			mockMvc.perform(post("/api/v1/auth/refresh").with(from("10.0.4.1")).contentType("application/json")
					.content("{\"refreshToken\":\"not-a-token-that-was-ever-issued\"}"))
					.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("REFRESH_TOKEN_INVALID"));
		}
	}
}