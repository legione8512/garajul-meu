package ro.garajulmeu.common;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gives every HTTP request a correlation identifier, per specification
 * section 27.
 *
 * <p>The identifier is placed in the SLF4J MDC, so Logback stamps it onto every
 * log line produced while handling that request without any code passing it
 * around. It is also returned in the {@value #HEADER} response header, so a
 * user reporting a problem can quote it and we can find that exact request
 * among all the others.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

	public static final String HEADER = "X-Request-Id";

	public static final String MDC_KEY = "requestId";

	private static final int MAX_LENGTH = 64;

	private static final Pattern SAFE_FORMAT = Pattern.compile("[A-Za-z0-9._-]+");

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {

		String requestId = resolve(request.getHeader(HEADER));
		MDC.put(MDC_KEY, requestId);
		response.setHeader(HEADER, requestId);
		try {
			chain.doFilter(request, response);
		}
		finally {
			// Threads are pooled and reused. Without this, the next request served
			// by this thread would inherit the previous request's identifier.
			MDC.remove(MDC_KEY);
		}
	}

	/**
	 * Honours a caller-supplied identifier only when it is short and made of safe
	 * characters, so a request can be traced across the mobile client and the API.
	 *
	 * <p>An unvalidated header would be a log-injection vector: newlines let an
	 * attacker forge log entries, and an unbounded value lets them flood storage.
	 */
	static String resolve(String suppliedHeader) {
		if (suppliedHeader != null
				&& suppliedHeader.length() <= MAX_LENGTH
				&& SAFE_FORMAT.matcher(suppliedHeader).matches()) {
			return suppliedHeader;
		}
		return UUID.randomUUID().toString();
	}
}