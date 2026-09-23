import {
  DEFAULT_LEADS, MAX_INSTALMENTS, type NewPayment, type PaymentDetails, type PaymentLead,
} from '../api/endpoints/payments.ts'
import { required, type Rule } from '../forms/rules.ts'

/**
 * The payment form's values. Everything is a string, as an input yields it,
 * except the choice of end and the leads, which are not typed.
 */
export interface PaymentValues {
  readonly kind: string
  readonly firstDueDate: string
  readonly frequency: string
  readonly endBy: 'date' | 'count'
  readonly lastDueDate: string
  readonly instalmentCount: string
  readonly leads: readonly PaymentLead[]
}

export type PaymentTextField = 'firstDueDate' | 'lastDueDate' | 'instalmentCount'

export const EMPTY_PAYMENT: PaymentValues = {
  kind: 'LOAN',
  firstDueDate: '',
  frequency: 'MONTHLY',
  endBy: 'count',
  lastDueDate: '',
  instalmentCount: '',
  leads: DEFAULT_LEADS,
}

/** A whole number from one to the backend's ceiling - nothing else is a count. */
const instalmentCount: Rule = {
  constraint: 'Range',
  test: (value) => /^\d+$/.test(value.trim())
    && Number(value) >= 1 && Number(value) <= MAX_INSTALMENTS,
  message: { key: 'validation.instalmentCount' },
}

/**
 * Only the end the person chose is checked, and only it is sent: the backend
 * refuses a series with both ends as firmly as one with none.
 */
export function paymentRules(values: PaymentValues) {
  return values.endBy === 'date'
    ? { firstDueDate: [required], lastDueDate: [required] }
    : { firstDueDate: [required], instalmentCount: [required, instalmentCount] }
}

export function textValuesOf(values: PaymentValues): Record<PaymentTextField, string> {
  return {
    firstDueDate: values.firstDueDate,
    lastDueDate: values.endBy === 'date' ? values.lastDueDate : '',
    instalmentCount: values.endBy === 'count' ? values.instalmentCount : '',
  }
}

export function paymentBody(values: PaymentValues): NewPayment {
  return {
    kind: values.kind,
    firstDueDate: values.firstDueDate,
    frequency: values.frequency,
    lastDueDate: values.endBy === 'date' ? values.lastDueDate : null,
    instalmentCount: values.endBy === 'count' ? Number(values.instalmentCount.trim()) : null,
    // Largest first, the order they fire in; none at all is the backend's default.
    remindDaysBefore: [...values.leads].sort((a, b) => b - a),
  }
}

/**
 * A stored payment, ready to correct. It comes back as a last date, which is
 * how the backend keeps every series whichever way it was entered.
 */
export function valuesOf(payment: PaymentDetails): PaymentValues {
  return {
    kind: payment.kind,
    firstDueDate: payment.firstDueDate,
    frequency: payment.frequency,
    endBy: 'date',
    lastDueDate: payment.lastDueDate,
    instalmentCount: '',
    leads: payment.remindDaysBefore.filter(isLead),
  }
}

function isLead(days: number): days is PaymentLead {
  return days === 7 || days === 3 || days === 1 || days === 0
}

/** The locale key for one lead. */
export function leadKey(lead: number) {
  switch (lead) {
    case 7: return 'payments.lead.days7' as const
    case 3: return 'payments.lead.days3' as const
    case 1: return 'payments.lead.day1' as const
    default: return 'payments.lead.onTheDay' as const
  }
}
