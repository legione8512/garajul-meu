import { documentPath } from './documents.ts'

/**
 * The one reminder endpoint section 16 names.
 *
 * <p>A path rather than a function, for the reason documents.ts gives: useResource
 * keys its effect on a string, and a string is stable between renders.
 */
export function remindersPath(vehicleId: string, documentId: string): string {
  return `${documentPath(vehicleId, documentId)}/reminders`
}

/**
 * The five states section 10.6 stores.
 *
 * <p>CANCELLED never arrives here - the endpoint filters it out, because a
 * schedule that was replaced answers no question a reader has - but it belongs
 * in the union so the switch that reads it is exhaustive rather than defaulted.
 * The same arrangement as NOT_CONFIGURED in DocumentStatus.
 */
export type ReminderStatus = 'PENDING' | 'PROCESSING' | 'SENT' | 'FAILED' | 'CANCELLED'

/**
 * One scheduled nudge.
 *
 * <p>`scheduledAt` is an instant, and the backend computed it from the account's
 * own timezone - so rendering it in the browser's zone shows the reader the time
 * they will actually be told at, rather than a number translated twice.
 *
 * <p>There is no identifier, and nothing needs one: a reminder is not something
 * a person opens, edits or deletes.
 */
export interface ReminderView {
  readonly offsetDays: number
  readonly scheduledAt: string
  readonly status: ReminderStatus
  readonly sentAt?: string | null
}