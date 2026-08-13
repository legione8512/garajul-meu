package ro.garajulmeu.common;

import ro.garajulmeu.common.RateLimitProperties.Policy;

/**
 * Counts attempts against a key and says whether this one is still allowed.
 *
 * <p><strong>Every implementation in V1 counts inside one JVM.</strong>
 * Specification section 19 permits that only while the backend runs as a single
 * Railway instance, and requires that the abstraction not imply otherwise. Two
 * instances would keep two independent counters, silently doubling every limit.
 * That is why this is an interface with an explicitly named in-memory
 * implementation: the day a second instance is introduced, this is the seam
 * where shared storage replaces it, and the change is visible rather than
 * forgotten.
 *
 * <p>Deliberately returns a boolean instead of throwing. Rate limiting is
 * infrastructure; which error code an exhausted budget produces is a decision
 * for the layer that knows what the caller was trying to do.
 */
public interface RateLimiter {

	/**
	 * Records one attempt against {@code key} and reports whether it fits inside
	 * the policy. An attempt is counted even when it is refused.
	 */
	boolean tryConsume(String key, Policy policy);
}