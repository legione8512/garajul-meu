package ro.garajulmeu.notification.dto;

import java.time.LocalTime;

/** What an account is told about, and when. Identical in and out. */
public record NotificationPreferencesView(
		boolean notificationsEnabled,
		boolean remind30Days,
		boolean remind14Days,
		boolean remind7Days,
		boolean remind3Days,
		boolean remind1Day,
		boolean remindOnExpiry,
		LocalTime notificationLocalTime) {
}