package ro.garajulmeu.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.servlet.http.Cookie;

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
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import ro.garajulmeu.TestcontainersConfiguration;
import ro.garajulmeu.auth.RefreshCookies;
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

	/** Where an iOS Capacitor WebView serves the application from. Not negotiable. */
	private static final String IOS_WEBVIEW_ORIGIN = "capacitor://localhost";

	/**
	 * A sibling subdomain of the production domain, and the reason the origin
	 * check is not redundant with {@code SameSite=Strict}. SameSite reasons about
	 * <em>sites</em> - the registrable domain - so a browser considers
	 * www.cyber-half.com and api.cyber-half.com the same site and attaches the
	 * refresh cookie to a request from one to the other. www is on shared hosting
	 * this project does not control.
	 */
	private static final String SIBLING_SUBDOMAIN = "https://www.cyber-half.com";

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

	/**
	 * Answers the question `application-prod.yml` wrote down and left open.
	 *
	 * <p>A Capacitor WebView on iOS is served from {@code capacitor://localhost}
	 * and <strong>that cannot be changed</strong>: the CLI's own documentation
	 * says the iOS scheme "can't be set to schemes that the WKWebView already
	 * handles, such as http or https". Android was given {@code https} in
	 * `capacitor.config.ts` and needs no help; iOS has no such option, so either
	 * Spring accepts a non-http origin or phase 18 has a problem no amount of
	 * frontend work can solve.
	 *
	 * <p>Asserted through the real filter chain rather than against
	 * {@code CorsConfiguration} directly, because what matters is whether a
	 * preflight <em>succeeds</em> end to end - the class accepting the string and
	 * the matcher agreeing about it are two different things, and only one of
	 * them is the question.
	 *
	 * <p>If this ever fails, the answer is not to widen the list: it is
	 * {@code allowedOriginPatterns}, and that trade would need writing down.
	 */
	@Test
	void aPreflightFromTheIosWebViewIsAllowed() throws Exception {
		mockMvc.perform(options("/api/v1/auth/login")
						.header(HttpHeaders.ORIGIN, IOS_WEBVIEW_ORIGIN)
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, IOS_WEBVIEW_ORIGIN))
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

	/**
	 * <strong>This is the CSRF defence section 14 asks for, and until now nobody
	 * had checked that it exists.</strong>
	 *
	 * <p>Section 14: "Web cookie-sensitive endpoints use strict allowed-origin
	 * checks and Spring CSRF protection appropriate for the selected cookie
	 * flow." The check is Spring's own CORS processor, which refuses an
	 * <em>actual</em> request from an unlisted origin and not merely its
	 * preflight - the handler is never reached. Every test above this one
	 * asserted only preflights, so the behaviour the whole design rests on was
	 * assumed rather than verified.
	 *
	 * <p>The forged request carries a refresh cookie on purpose. A cross-site
	 * attacker cannot read a response, but they do not need to: making the
	 * refresh succeed rotates the token, so the legitimate client's next refresh
	 * presents a spent one, {@code RefreshTokenService} treats that as theft, and
	 * the whole family is revoked. The victim is signed out of every device
	 * without anyone stealing anything.
	 *
	 * <p>{@code SameSite=Strict} does not close this. It reasons about sites, and
	 * a request from www.cyber-half.com to api.cyber-half.com is same-site, so
	 * the cookie is attached. The origin check is what distinguishes them.
	 */
	@Test
	void aForgedRefreshFromASiblingSubdomainIsRefusedBeforeItReachesTheHandler() throws Exception {
		mockMvc.perform(post("/api/v1/auth/refresh")
						.header(HttpHeaders.ORIGIN, SIBLING_SUBDOMAIN)
						.cookie(new Cookie(RefreshCookies.NAME, "a-stolen-ride-on-somebody-elses-session"))
						.contentType("application/json")
						.content("{}"))
				.andExpect(status().isForbidden());
	}

	/**
	 * The control for the test above, and the reason its 403 means what it
	 * claims. Byte for byte the same forged request, changing only the origin:
	 * this one gets past CORS and is refused by the token instead, with 401.
	 *
	 * <p>Without this, a 403 from any cause at all - a security rule, a filter,
	 * a typo in the path - would read as proof the origin check worked.
	 */
	@Test
	void theSameRequestFromTheApplicationOriginIsRefusedByTheTokenAndNotByTheOrigin() throws Exception {
		mockMvc.perform(post("/api/v1/auth/refresh")
						.header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
						.cookie(new Cookie(RefreshCookies.NAME, "a-stolen-ride-on-somebody-elses-session"))
						.contentType("application/json")
						.content("{}"))
				.andExpect(status().isUnauthorized());
	}

	/**
	 * <strong>The origin check only works on methods that carry an origin.</strong>
	 *
	 * <p>Browsers send {@code Origin} on every POST, which is what lets CORS
	 * refuse a forged one. They send none on a GET triggered by an {@code <img>},
	 * a stylesheet or a plain link - and a request with no origin is not a CORS
	 * request at all, so the processor passes it through untouched. Combined with
	 * SameSite's site-level blindness, a GET endpoint that read the refresh
	 * cookie would be forgeable from any subdomain with nothing in the way.
	 *
	 * <p>Both endpoints are POST today and neither is likely to change. This
	 * exists because the consequence of changing one is silent: no test would
	 * fail, no warning would appear, and the CSRF protection this file documents
	 * would simply stop applying to that route.
	 */
	@Test
	void nothingReadsTheRefreshCookieOnAMethodThatCarriesNoOrigin() {
		List<String> unsafe = new ArrayList<>();

		handlerMapping.getHandlerMethods().forEach((info, handler) -> {
			if (!readsTheRefreshCookie(handler)) {
				return;
			}

			Set<RequestMethod> methods = info.getMethodsCondition().getMethods();

			if (methods.isEmpty() || !methods.stream().allMatch(RequestMethod.POST::equals)) {
				unsafe.add(patternsOf(info) + " serves " + (methods.isEmpty() ? "ANY METHOD" : methods.toString()));
			}
		});

		assertThat(unsafe)
				.as("routes reading the refresh cookie on a method a browser sends without an Origin header")
				.isEmpty();
	}

	/**
	 * The CORS configuration is registered for {@code /api/**} and nowhere else,
	 * so a cookie endpoint outside that prefix would have no origin check at all.
	 */
	@Test
	void everyRouteReadingTheRefreshCookieSitsUnderTheCorsConfiguration() {
		List<String> outside = new ArrayList<>();

		handlerMapping.getHandlerMethods().forEach((info, handler) -> {
			if (readsTheRefreshCookie(handler) && !underApi(info)) {
				outside.add(patternsOf(info).toString());
			}
		});

		assertThat(outside)
				.as("routes reading the refresh cookie from outside the /api/** the CORS filter covers")
				.isEmpty();
	}

	private static boolean readsTheRefreshCookie(HandlerMethod handler) {
		return Arrays.stream(handler.getMethodParameters())
				.map(parameter -> parameter.getParameterAnnotation(CookieValue.class))
				.filter(Objects::nonNull)
				.anyMatch(cookie -> RefreshCookies.NAME.equals(cookie.name())
						|| RefreshCookies.NAME.equals(cookie.value()));
	}

	private static Set<String> patternsOf(RequestMappingInfo info) {
		PathPatternsRequestCondition patterns = info.getPathPatternsCondition();
		return patterns == null ? Set.of() : patterns.getPatternValues();
	}

	private Set<String> methodsServedUnderApi() {
		return handlerMapping.getHandlerMethods().keySet().stream()
				.filter(CorsTest::underApi)
				.flatMap(info -> info.getMethodsCondition().getMethods().stream())
				.map(Enum::name)
				.collect(Collectors.toCollection(TreeSet::new));
	}

	private static boolean underApi(RequestMappingInfo info) {
		return patternsOf(info).stream().anyMatch(pattern -> pattern.startsWith("/api/"));
	}
}