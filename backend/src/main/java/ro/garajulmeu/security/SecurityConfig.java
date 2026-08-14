package ro.garajulmeu.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import ro.garajulmeu.common.RequestIdFilter;

/**
 * Baseline security for the API. Specification section 14.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	/**
	 * Argon2 is the frozen algorithm choice. The factory method pins the
	 * parameters chosen and tested by the Spring Security team - 16 MB of
	 * memory, two iterations, parallelism one, a sixteen byte salt.
	 */
	@Bean
	PasswordEncoder passwordEncoder() {
		return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
	}

	/**
	 * One symmetric key signs and verifies, because this application both issues
	 * and consumes its own tokens. An asymmetric key pair only earns its extra
	 * complexity when a separate service must verify without being able to sign.
	 */
	@Bean
	JwtEncoder jwtEncoder(JwtProperties properties) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(signingKey(properties)));
	}

	/**
	 * Declaring this bean also silences Spring Boot's development user: its
	 * auto-configuration backs off as soon as a JwtDecoder exists, so the
	 * "Using generated security password" line disappears.
	 */
	@Bean
	JwtDecoder jwtDecoder(JwtProperties properties) {
		return NimbusJwtDecoder.withSecretKey(signingKey(properties))
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
	}

	private static SecretKey signingKey(JwtProperties properties) {
		return new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	/**
	 * Section 21: an explicit allowlist, with credentials enabled because the
	 * refresh cookie and the bearer token both have to travel.
	 *
	 * <p>Exposing {@link RequestIdFilter#HEADER} is not a nicety. A browser hides
	 * every cross-origin response header from JavaScript except a short standard
	 * set, so without this the correlation identifier never reaches the frontend
	 * and the request id on an error screen would always be blank - which would
	 * quietly waste the whole correlation mechanism.
	 *
	 * <p>Scoped to {@code /api/**}. The health endpoint is not called from a
	 * browser and has no reason to answer preflights.
	 */
	@Bean
	CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(properties.allowedOrigins());
		configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		configuration.setExposedHeaders(List.of(RequestIdFilter.HEADER));
		configuration.setAllowCredentials(true);
		// An hour of preflight caching. Every cross-origin call that carries an
		// Authorization header needs one, so without this each request costs two.
		configuration.setMaxAge(Duration.ofHours(1));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", configuration);
		return source;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http,
			ApiErrorAuthenticationEntryPoint authenticationEntryPoint) throws Exception {
		return http
				// Picks up the CorsConfigurationSource bean above, and places the
				// CORS filter ahead of authorisation - so a preflight, which carries
				// no Authorization header by definition, is answered rather than
				// refused with a 401 the browser would report as a CORS failure.
				.cors(Customizer.withDefaults())

				.csrf(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)

				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

				// Without an explicit entry point a credential-less request answers
				// 403 rather than 401. Ours also gives it the ApiErrorResponse body,
				// so the frontend has a code to translate like everywhere else.
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(authenticationEntryPoint))

				// Reads the Authorization: Bearer header, verifies the signature and
				// the expiry, and populates the security context. Spring provides the
				// whole filter; we only supply the decoder above.
				//
				// The entry point has to be set here as well as in exceptionHandling
				// above, because these are two different paths to a 401. A missing
				// token is refused later by AuthorizationFilter and handled by
				// ExceptionTranslationFilter, which uses the global entry point. A
				// token that fails to decode is refused by this filter, which calls
				// its own entry point directly and never reaches the other one.
				// Spring's default here also answers with a WWW-Authenticate header
				// carrying an English error_description and the server host - English
				// prose the frontend must own, per section 6, and internal detail a
				// response has no place for: section 17 gives a failure one code.
				.oauth2ResourceServer(oauth2 -> oauth2
						.authenticationEntryPoint(authenticationEntryPoint)
						.jwt(Customizer.withDefaults()))

				.authorizeHttpRequests(requests -> requests
						.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
						.requestMatchers("/api/v1/auth/**").permitAll()
						.anyRequest().authenticated())

				.build();
	}
}