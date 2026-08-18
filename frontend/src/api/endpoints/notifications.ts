import { apiFetch } from '../client.ts'

export const notificationPreferencesPath = '/api/v1/users/me/notification-preferences'

/**
 * What an account is told about, and when. Identical in and out, which is why
 * one interface serves both directions.
 *
 * <p><strong>Every field is required on the way back.</strong> Section 16 calls
 * this endpoint a replace and the backend enforces it with boxed booleans: a
 * body that omits a switch is refused rather than read as an instruction to
 * turn that reminder off. So the screen must always send all eight.
 */
export interface NotificationPreferences {
  readonly notificationsEnabled: boolean
  readonly remind30Days: boolean
  readonly remind14Days: boolean
  readonly remind7Days: boolean
  readonly remind3Days: boolean
  readonly remind1Day: boolean
  readonly remindOnExpiry: boolean
  /** `HH:mm:ss` on the wire; `<input type="time">` speaks `HH:mm`. */
  readonly notificationLocalTime: string
}

export function saveNotificationPreferences(
  preferences: NotificationPreferences,
): Promise<NotificationPreferences> {
  return apiFetch<NotificationPreferences>(notificationPreferencesPath, {
    method: 'PUT',
    body: JSON.stringify(preferences),
  })
}