package ro.garajulmeu.common;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import ro.garajulmeu.common.RateLimitProperties.Policy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * No Spring and no sleeping: the clock is moved by hand, so a fifteen-minute
 * window is verified in microseconds.
 */
class InMemoryRateLimiterTest {

	private static final Policy THREE_PER_MINUTE = new Policy(3, Duration.ofMinutes(1));

	private final MutableClock clock = new MutableClock(Instant.parse("2026-08-14T09:00:00Z"));

	private final InMemoryRateLimiter limiter = new InMemoryRateLimiter(clock);

	@Test
	void allowsExactlyTheConfiguredNumberOfAttempts() {
		assertThat(limiter.tryConsume("caller", THREE_PER_MINUTE)).isTrue();
		assertThat(limiter.tryConsume("caller", THREE_PER_MINUTE)).isTrue();
		assertThat(limiter.tryConsume("caller", THREE_PER_MINUTE)).isTrue();

		assertThat(limiter.tryConsume("caller", THREE_PER_MINUTE)).isFalse();
	}

	@Test
	void theWindowReopensOnceItHasElapsed() {
		for (int attempt = 0; attempt < 4; attempt++) {
			limiter.tryConsume("caller", THREE_PER_MINUTE);
		}

		clock.advance(Duration.ofSeconds(61));

		assertThat(limiter.tryConsume("caller", THREE_PER_MINUTE)).isTrue();
	}

	/**
	 * Hammering must not push the window forward. If it did, a refused caller
	 * could keep their own lockout alive for as long as they kept retrying.
	 */
	@Test
	void hammeringDoesNotExtendTheLockout() {
		for (int attempt = 0; attempt < 3; attempt++) {
			limiter.tryConsume("caller", THREE_PER_MINUTE);
		}

		clock.advance(Duration.ofSeconds(59));
		assertThat(limiter.tryConsume("caller", THREE_PER_MINUTE)).isFalse();

		clock.advance(Duration.ofSeconds(2));
		assertThat(limiter.tryConsume("caller", THREE_PER_MINUTE)).isTrue();
	}

	@Test
	void keysDoNotShareABudget() {
		for (int attempt = 0; attempt < 4; attempt++) {
			limiter.tryConsume("one", THREE_PER_MINUTE);
		}

		assertThat(limiter.tryConsume("one", THREE_PER_MINUTE)).isFalse();
		assertThat(limiter.tryConsume("another", THREE_PER_MINUTE)).isTrue();
	}

	/** Otherwise the limiter is itself a way to exhaust memory. */
	@Test
	void expiredEntriesAreSweptSoTheMapDoesNotGrowForever() {
		for (int caller = 0; caller < 500; caller++) {
			limiter.tryConsume("caller-" + caller, THREE_PER_MINUTE);
		}
		assertThat(limiter.trackedKeys()).isEqualTo(500);

		clock.advance(Duration.ofMinutes(2));
		limiter.tryConsume("someone-new", THREE_PER_MINUTE);

		assertThat(limiter.trackedKeys()).isEqualTo(1);
	}

	private static final class MutableClock extends Clock {

		private Instant now;

		private MutableClock(Instant now) {
			this.now = now;
		}

		private void advance(Duration amount) {
			this.now = this.now.plus(amount);
		}

		@Override
		public Instant instant() {
			return now;
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			throw new UnsupportedOperationException();
		}
	}
}