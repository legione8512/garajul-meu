package ro.garajulmeu.common;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

import ro.garajulmeu.common.RateLimitProperties.Policy;

/**
 * A fixed-window counter per key, held in this JVM. See {@link RateLimiter} for
 * why that is acceptable in V1 and what has to change when it stops being.
 *
 * <p>Fixed window rather than a token bucket because the requirement is simply
 * "this many attempts in this much time". A bucket earns its extra machinery
 * when a caller needs a controlled burst on top of a steady refill rate, which
 * no authentication endpoint does.
 */
@Component
public class InMemoryRateLimiter implements RateLimiter {

	private static final Duration SWEEP_INTERVAL = Duration.ofMinutes(1);

	private final Clock clock;

	private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

	private final AtomicLong nextSweepAt;

	InMemoryRateLimiter(Clock clock) {
		this.clock = clock;
		this.nextSweepAt = new AtomicLong(clock.millis() + SWEEP_INTERVAL.toMillis());
	}

	@Override
	public boolean tryConsume(String key, Policy policy) {
		long now = clock.millis();
		sweepIfDue(now);

		// compute is atomic per key, so two threads racing on the same key cannot
		// both read the same count and each write count + 1.
		Window updated = windows.compute(key, (ignoredKey, current) -> {
			if (current == null || now >= current.expiresAt()) {
				return new Window(now + policy.window().toMillis(), 1);
			}
			// Capped one past the limit. A caller who keeps hammering gains
			// nothing by it, and the counter cannot climb towards overflow. The
			// window still ends exactly when it was always going to - hammering
			// must not extend it, or a refused caller could lock themselves out
			// indefinitely.
			return new Window(current.expiresAt(), Math.min(current.count() + 1, policy.limit() + 1));
		});

		return updated.count() <= policy.limit();
	}

	/**
	 * Drops entries whose window has ended. Without this the map would keep one
	 * entry per key ever seen, which turns the limiter itself into a way to
	 * exhaust memory.
	 */
	private void sweepIfDue(long now) {
		long due = nextSweepAt.get();
		if (now < due) {
			return;
		}

		// Whoever wins the swap does the sweep; every other thread carries on
		// rather than queueing behind it.
		if (!nextSweepAt.compareAndSet(due, now + SWEEP_INTERVAL.toMillis())) {
			return;
		}

		windows.values().removeIf(window -> now >= window.expiresAt());
	}

	/** How many keys are currently held. Exists for the memory-growth test. */
	int trackedKeys() {
		return windows.size();
	}

	private record Window(long expiresAt, int count) {
	}
}