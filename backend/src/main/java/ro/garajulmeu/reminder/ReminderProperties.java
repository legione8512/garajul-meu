package ro.garajulmeu.reminder;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * How the scheduler behaves. Specification sections 12 and 19.
 *
 * <p><strong>{@code schedulerEnabled} is bound here and read nowhere.</strong>
 * The condition that actually switches scheduling on reads the raw environment,
 * not this record - so this field changes no behaviour at all. It exists because
 * a key with no {@code @ConfigurationProperties} class behind it is an "Unknown
 * property" warning in the IDE, which is how two earlier phases discovered that
 * a seam had been half-added.
 *
 * @param batchSize         how many due reminders one pass claims. A ceiling on
 *                          how long a single pass can hold rows in PROCESSING,
 *                          not a throughput limit - the next pass takes the rest.
 * @param maxAttempts       how many passes a reminder gets before it is written
 *                          off. Retries happen at the polling interval with no
 *                          backoff, which is a V1 simplification rather than an
 *                          oversight: at one minute apart, three attempts span
 *                          three minutes and a provider outage longer than that
 *                          is not something a delay of thirty seconds would have
 *                          saved.
 * @param staleAfterMinutes how long a reminder may sit in PROCESSING before it
 *                          is assumed abandoned. Section 12 has the scheduler
 *                          querying PENDING work only, so without this a crash
 *                          mid-send would strand a reminder permanently - which
 *                          is precisely the "short restart loses a reminder"
 *                          that section forbids.
 */
@ConfigurationProperties(prefix = "garajul-meu.reminder")
public record ReminderProperties(
		@DefaultValue("false") boolean schedulerEnabled,
		@DefaultValue("60") int pollIntervalSeconds,
		@DefaultValue("50") int batchSize,
		@DefaultValue("3") int maxAttempts,
		@DefaultValue("10") int staleAfterMinutes) {
}