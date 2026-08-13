package ro.garajulmeu.common;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Time as an injected dependency rather than a static call.
 *
 * <p>{@code Instant.now()} cannot be moved forward in a test, so anything that
 * reasons about elapsed time becomes either untestable or slow. Phase 11 needs
 * this far more than the rate limiter does: reminder scheduling has to be
 * verified across timezones and day boundaries, and no test can wait for
 * tomorrow.
 */
@Configuration
public class TimeConfig {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}
}