package ro.garajulmeu.reminder.dto;

import java.time.Instant;

import ro.garajulmeu.reminder.ReminderStatus;

/**
 * One scheduled nudge, as a reader sees it. Specification section 16's
 * "read scheduled reminder view".
 *
 * <p>Four fields out of the entity's eleven. {@code attempt_count} and
 * {@code last_error_code} are operational - they explain a failure to whoever
 * has to fix it, and mean nothing to the person who owns the car. They stay in
 * the table, where support can read them.
 *
 * @param scheduledAt an instant, so the client renders it in the reader's own
 *                    zone. The backend computed it from that zone in the first
 *                    place, which is why nine in the morning stays nine in the
 *                    morning on the screen.
 * @param sentAt      null until it has been. Never a promise: for the whole of
 *                    V1 web this is set on reminders that reached no device at
 *                    all, because section 18 makes push native-only.
 */
public record ReminderView(int offsetDays, Instant scheduledAt, ReminderStatus status,
		Instant sentAt) {
}