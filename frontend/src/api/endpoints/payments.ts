import { apiFetch } from '../client.ts'

/**
 * A vehicle's recurring payments, 1.1: the car-loan or leasing instalment and
 * the CASCO instalment, dates only (docs/DESIGN_1_1_INSTALMENTS.md).
 *
 * <p>Reads are paths and writes are functions, as documents.ts does.
 */
export function paymentsPath(vehicleId: string): string {
  return `/api/v1/vehicles/${vehicleId}/payments`
}

export function paymentPath(vehicleId: string, paymentId: string): string {
  return `${paymentsPath(vehicleId)}/${paymentId}`
}

export const paymentKinds = ['LOAN', 'CASCO'] as const
export type PaymentKind = (typeof paymentKinds)[number]

export const paymentFrequencies = ['MONTHLY', 'QUARTERLY', 'SEMIANNUAL', 'ANNUAL'] as const
export type PaymentFrequency = (typeof paymentFrequencies)[number]

/** The leads a person can choose, largest first - the backend's order. */
export const paymentLeads = [7, 3, 1, 0] as const
export type PaymentLead = (typeof paymentLeads)[number]

/** The owner's default when nothing is chosen: three days before, and one. */
export const DEFAULT_LEADS: readonly PaymentLead[] = [3, 1]

/** The backend's ceiling: ten years of monthly instalments. */
export const MAX_INSTALMENTS = 120

/**
 * One payment as its owner reads it. The next instalment is worked out by the
 * backend in the reader's own day; all three `next` fields are null once the
 * last instalment has passed.
 */
export interface PaymentDetails {
  readonly id: string
  readonly kind: PaymentKind
  readonly firstDueDate: string
  readonly frequency: PaymentFrequency
  readonly lastDueDate: string
  readonly instalmentCount: number
  readonly remindDaysBefore: readonly number[]
  readonly nextDueDate?: string | null
  readonly nextInstalment?: number | null
  readonly daysUntilNext?: number | null
}

/**
 * What adding and correcting send. Exactly one of `lastDueDate` and
 * `instalmentCount` is set; the backend refuses both and neither.
 */
export interface NewPayment {
  readonly kind: string
  readonly firstDueDate: string
  readonly frequency: string
  readonly lastDueDate: string | null
  readonly instalmentCount: number | null
  readonly remindDaysBefore: readonly number[]
}

export function addPayment(vehicleId: string, payment: NewPayment): Promise<PaymentDetails> {
  return apiFetch<PaymentDetails>(paymentsPath(vehicleId), {
    method: 'POST',
    body: JSON.stringify(payment),
  })
}

/** A correction replaces the whole schedule, and the backend regenerates its reminders. */
export function correctPayment(
  vehicleId: string, paymentId: string, payment: NewPayment,
): Promise<PaymentDetails> {
  return apiFetch<PaymentDetails>(paymentPath(vehicleId, paymentId), {
    method: 'PATCH',
    body: JSON.stringify(payment),
  })
}

export function deletePayment(vehicleId: string, paymentId: string): Promise<void> {
  return apiFetch<void>(paymentPath(vehicleId, paymentId), { method: 'DELETE' })
}
