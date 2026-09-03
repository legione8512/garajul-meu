import { apiFetch } from '../client.ts'

/**
 * Section 10.7 stores native registrations only - there is no WEB platform,
 * because V1 implements no Firebase Web Push and a third value would invite
 * storing something the delivery path cannot reach.
 */
export type DevicePlatform = 'ANDROID' | 'IOS'

/**
 * A registered device as the server describes it back.
 *
 * <p><strong>No token, in any form.</strong> The client that registered it
 * already holds it, and sending it back would put a live credential in a
 * response body and every cache between here and there in exchange for nothing.
 */
export interface DeviceView {
  readonly id: string
  readonly platform: DevicePlatform
  readonly deviceName: string | null
  readonly notificationsEnabled: boolean
  readonly tokenUpdatedAt: string
}

export interface DeviceRegistration {
  readonly platform: DevicePlatform
  readonly pushToken: string
  readonly deviceName?: string
  /**
   * Whether this installation can show a notification **right now**.
   *
   * <p>Required, and the backend refuses a body without it. The failure it
   * closes is a lie rather than an inconvenience: a permission revoked after
   * registration leaves the token perfectly valid, so Firebase accepts the
   * message, the dispatcher records the reminder as sent, and the person is
   * shown nothing. Reporting the truth on every launch is what keeps the record
   * honest, and it costs no extra request because this endpoint is called on
   * every launch anyway.
   */
  readonly notificationsEnabled: boolean
}

/**
 * An upsert, which is why it answers 200 and not 201: the usual answer is "the
 * registration you already had". Called at every launch rather than once, so
 * that `notificationsEnabled` is a fact about now rather than about the day
 * somebody first agreed.
 */
export function registerDevice(registration: DeviceRegistration): Promise<DeviceView> {
  return apiFetch<DeviceView>('/api/v1/devices', {
    method: 'POST',
    body: JSON.stringify(registration),
  })
}

export function unregisterDevice(deviceId: string): Promise<void> {
  return apiFetch<void>(`/api/v1/devices/${deviceId}`, { method: 'DELETE' })
}