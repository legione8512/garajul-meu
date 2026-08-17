package ro.garajulmeu.reminder;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * One pass over everything that is due. Specification sections 12 and 19.
 *
 * <p><strong>This bean exists even when scheduling is switched off.</strong> The
 * {@code @Scheduled} annotation is only read by the post-processor that
 * {@code @EnableScheduling} registers, so without it this class is an ordinary
 * bean with an ordinary method - which is exactly what the tests call. The
 * alternative, conditioning the bean itself, would have made the one method
 * worth testing unreachable from a test.
 *
 * <p><strong>Single-instance, per section 19.</strong> No distributed lock, and
 * deliberately none: the specification names ShedLock as something to design
 * before horizontal scaling, not something to add now. What protects against a
 * duplicate message is the unique constraint on the delivery, which is true
 * whatever runs the scheduler.
 */
@Component
public class ReminderScheduler {

	private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

	private final ReminderDispatcher dispatcher;

	ReminderScheduler(ReminderDispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}

	/**
	 * {@code fixedDelay} rather than {@code fixedRate}: the interval is measured
	 * from the end of one pass, so a slow pass delays the next rather than
	 * overlapping it. With a single-threaded scheduler that overlap could not
	 * happen anyway, and this is one fewer thing depending on that.
	 */
	@Scheduled(fixedDelayString = "${garajul-meu.reminder.poll-interval-seconds}",
			timeUnit = TimeUnit.SECONDS)
	void tick() {
		runOnce();
	}

	/**
	 * Release, claim, send. Returns how many reminders were claimed.
	 *
	 * <p>Each reminder is dispatched inside its own try: one that throws must not
	 * take the rest of the batch with it. It stays in PROCESSING, and
	 * {@link ReminderDispatcher#releaseStalled()} is what brings it back - which
	 * is why release runs first rather than last.
	 */
	public int runOnce() {
		dispatcher.releaseStalled();

		List<DueReminder> due = dispatcher.claimDue();

		for (DueReminder reminder : due) {
			try {
				dispatcher.dispatch(reminder);
			} catch (RuntimeException exception) {
				log.error("Reminder {} could not be dispatched", reminder.reminderId(), exception);
			}
		}

		return due.size();
	}
}