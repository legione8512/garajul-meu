package ro.garajulmeu.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Baseline security for the API. Specification section 14.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	/**
	 * Argon2 is the frozen algorithm choice. The factory method pins the
	 * parameters chosen and tested by the Spring Security team - 16 MB of
	 * memory, two iterations, parallelism one, a sixteen byte salt - rather than
	 * leaving us to invent cost parameters.
	 *
	 * <p>Requires BouncyCastle on the classpath; the encoder delegates the actual
	 * Argon2 computation to it.
	 */
	@Bean
	PasswordEncoder passwordEncoder() {
		return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				// This API authenticates with a bearer token, not a session cookie,
				// so there is no ambient authority for CSRF to exploit. The cookie
				// based refresh and logout endpoints arrive in 4.5 and will need
				// CSRF protection switched back on for their paths specifically.
				.csrf(AbstractHttpConfigurer::disable)

				// Spring's own login page, browser popup and logout endpoint are
				// replaced by our /api/v1/auth endpoints.
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)

				// No HTTP session is ever created. Every request proves who it is
				// on its own, which is what lets the API scale and be called from
				// the native apps identically.
				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

				// Without an explicit entry point, a request with no credentials
				// would answer 403. 401 is the correct answer to "you did not
				// authenticate", and the frontend distinguishes the two.
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))

				.authorizeHttpRequests(requests -> requests
						.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
						.anyRequest().authenticated())

				.build();
	}
}