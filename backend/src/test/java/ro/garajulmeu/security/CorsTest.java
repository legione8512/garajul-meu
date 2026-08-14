package ro.garajulmeu.security;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import ro.garajulmeu.TestcontainersConfiguration;
import ro.garajulmeu.common.RequestIdFilter;
import ro.garajulmeu.email.EmailProvider;

/**
 * Annotations identical to AuthFlowTest, including the unused EmailProvider
 * mock, so the cached context is reused and no further container starts. The
 * allowed origin comes from src/test/resources/application.yml for the same
 * reason: a @TestPropertySource here would change the context cache key.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class CorsTest {

	private static final String ALLOWED_ORIGIN = "http://localhost:5173";

	@Autowired
	private MockMvc mockMvc;

	/** Unused. Present only so this class shares AuthFlowTest's context. */
	@MockitoBean
	private EmailProvider emailProvider;

	@Test
	void aPreflightFromTheApplicationOriginMayCarryCredentials() throws Exception {
		mockMvc.perform(options("/api/v1/auth/login")
						.header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
	}

	@Test
	void aPreflightFromAnywhereElseIsRefused() throws Exception {
		mockMvc.perform(options("/api/v1/auth/login")
						.header(HttpHeaders.ORIGIN, "https://not-our-application.example.com")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
				.andExpect(status().isForbidden());
	}

	/**
	 * Asserted on a real request that <em>fails</em>, deliberately. The CORS
	 * headers have to be present on error responses too: without them a browser
	 * hides the body, and the frontend could not read the error code or the
	 * request id from the very responses that need explaining.
	 */
	@Test
	void anUnauthenticatedApiCallStillCarriesTheCorsHeadersAndExposesTheRequestId() throws Exception {
		mockMvc.perform(get("/api/v1/users/me")
						.header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
						containsString(RequestIdFilter.HEADER)));
	}
}