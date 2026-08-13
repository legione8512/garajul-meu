package ro.garajulmeu.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

import ro.garajulmeu.common.RequestIdFilter;
import ro.garajulmeu.exception.ApiErrorResponse;
import ro.garajulmeu.exception.ErrorCode;

/**
 * Answers an unauthenticated request with the same {@link ApiErrorResponse}
 * shape every other failure uses.
 *
 * <p>This exists because authentication fails inside the servlet filter chain,
 * before any controller is reached. {@code @RestControllerAdvice} never sees it,
 * so {@code GlobalExceptionHandler} cannot help here - which is why the previous
 * {@code HttpStatusEntryPoint} returned a bare 401 with no body, leaving the
 * frontend with no code to translate.
 *
 * <p>The injected mapper is the one Spring Boot auto-configures and Spring MVC
 * uses, not a fresh instance, so this response serialises identically to every
 * other error - the same {@code Instant} format, the same field order.
 */
@Component
public class ApiErrorAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private static final Logger log = LoggerFactory.getLogger(ApiErrorAuthenticationEntryPoint.class);

	private static final ErrorCode CODE = ErrorCode.AUTHENTICATION_REQUIRED;

	private final ObjectMapper objectMapper;

	ApiErrorAuthenticationEntryPoint(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authenticationException) throws IOException {

		// Method and path only. The exception carries detail about why a bearer
		// token failed, and specification section 27 keeps token material out of
		// the log entirely. An unauthenticated request is an ordinary outcome, so
		// INFO, not ERROR - section 27 again, so Sentry stays quiet from Phase 15.
		log.info("Unauthenticated request to {} {}", request.getMethod(), request.getRequestURI());

		response.setStatus(CODE.status().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);

		// The MDC entry is present because RequestIdFilter runs at
		// HIGHEST_PRECEDENCE, outside the whole security chain.
		objectMapper.writeValue(response.getOutputStream(),
				ApiErrorResponse.of(CODE, request.getRequestURI(), MDC.get(RequestIdFilter.MDC_KEY)));
	}
}