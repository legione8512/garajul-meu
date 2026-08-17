package ro.garajulmeu.reminder;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Switches Spring's scheduler on, and nothing else.
 *
 * <p>Separate from {@link ReminderScheduler} so that turning scheduling off does
 * not also remove the bean whose method the tests call. Off in tests, and that
 * is not a detail: with it on, every Spring test in the project would start
 * dispatching real reminders against its own container while it ran.
 *
 * <p>An explicit property rather than a profile, matching the three provider
 * seams. Unlike those, an absent property here means "do not schedule" rather
 * than a refusal to start - a backend running without the scheduler is a
 * legitimate way to run one, and section 19 expects exactly one instance that
 * does.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "garajul-meu.reminder.scheduler-enabled", havingValue = "true")
public class ReminderSchedulingConfig {
}