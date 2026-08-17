package ro.garajulmeu.notification.dto;

import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

/**
 * Section 16 calls this endpoint "replace/update", and it replaces: every field
 * is required, and a body that omits one is refused rather than half-applied.
 *
 * <p>Boxed booleans on purpose. A primitive would arrive as false for a field
 * the client forgot, and the request would be accepted as an instruction to turn
 * that reminder off - the one failure mode a preferences screen must not have.
 */
public record SaveNotificationPreferencesRequest(

		@NotNull Boolean notificationsEnabled,
		@NotNull Boolean remind30Days,
		@NotNull Boolean remind14Days,
		@NotNull Boolean remind7Days,
		@NotNull Boolean remind3Days,
		@NotNull Boolean remind1Day,
		@NotNull Boolean remindOnExpiry,
		@NotNull LocalTime notificationLocalTime) {
}