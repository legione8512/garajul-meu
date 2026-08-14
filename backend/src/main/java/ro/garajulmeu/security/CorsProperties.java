package ro.garajulmeu.security;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Which origins the browser is allowed to call this API from. Specification
 * section 21.
 *
 * <p>An empty list is a valid, and safe, state: no browser origin is permitted
 * and the API still serves native clients, which are not bound by CORS at all.
 * Failing startup instead would refuse to run a perfectly usable configuration.
 *
 * @param allowedOrigins exact origins. Never a wildcard - the CORS specification
 *                       forbids combining one with credentials, and every
 *                       request this API answers is credentialed
 */
@ConfigurationProperties(prefix = "garajul-meu.cors")
public record CorsProperties(List<String> allowedOrigins) {

	public CorsProperties {
		allowedOrigins = allowedOrigins != null ? List.copyOf(allowedOrigins) : List.of();
	}
}