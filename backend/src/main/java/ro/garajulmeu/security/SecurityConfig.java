package ro.garajulmeu.security;

import java.nio.charset.StandardCharsets;

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

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http,
			ApiErrorAuthenticationEntryPoint authenticationEntryPoint) throws Exception {
		return http
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
				// prose the frontend must own, per section 6, and internal detail
				// section 30 keeps out of responses.
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