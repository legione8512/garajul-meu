package ro.garajulmeu.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

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

	/**
	 * The application's route table, read rather than described.
	 *
	 * <p>Qualified by name because Actuator contributes a second bean of this
	 * type, {@code controllerEndpointHandlerMapping}, and that is a different
	 * route table entirely. Taking the wrong one would leave this test checking
	 * the methods of the management endpoints while claiming to check ours - a
	 * green test asserting nothing about the thing it is named after.
	 */
	@Autowired
	@Qualifier("requestMappingHandlerMapping")
	private RequestMappingHandlerMapping handlerMapping;

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

	/**
	 * <strong>Every method the controllers actually serve must survive a
	 * preflight</strong>, and the list of methods is read from Spring's route
	 * table rather than written out here.
	 *
	 * <p>Written on 2026-08-19, after a browser walkthrough found that PUT was
	 * missing from the allowed methods. Two endpoints had been unreachable from a
	 * browser for two phases - the notification preferences save since 11.1 and
	 * the whole vehicle image upload since 12.3 - while every test stayed green,
	 * because the only preflight anybody had asserted was for POST.
	 *
	 * <p>Deriving the set is the entire point. A test naming the methods would
	 * have to be remembered and updated, which is the same act of remembering
	 * that failed; a test that reads the mappings fails on the day an endpoint
	 * with an unlisted method is added. The same shape as `coordinates.test.ts`
	 * checking that every field has a position, and `errorKey.test.ts` checking
	 * both directions.
	 *
	 * <p>The path is arbitrary. The configuration is registered for
	 * {@code /api/**} and the CORS filter consults it by path, not by whether a
	 * handler exists for that method there.
	 */
	@Test
	void everyMethodTheApiActuallyServesSurvivesAPreflight() throws Exception {
		List<String> refused = new ArrayList<>();

		for (String method : methodsServedUnderApi()) {
			int status = mockMvc.perform(options("/api/v1/users/me")
							.header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
							.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, method))
					.andReturn().getResponse().getStatus();

			if (status != 200) {
				refused.add(method + " → " + status);
			}
		}

		assertThat(refused)
				.as("methods the API serves but CORS refuses")
				.isEmpty();
	}

	private Set<String> methodsServedUnderApi() {
		return handlerMapping.getHandlerMethods().keySet().stream()
				.filter(CorsTest::underApi)
				.flatMap(info -> info.getMethodsCondition().getMethods().stream())
				.map(Enum::name)
				.collect(Collectors.toCollection(TreeSet::new));
	}

	private static boolean underApi(RequestMappingInfo info) {
		PathPatternsRequestCondition patterns = info.getPathPatternsCondition();

		return patterns != null && patterns.getPatternValues().stream()
				.anyMatch(pattern -> pattern.startsWith("/api/"));
	}
}