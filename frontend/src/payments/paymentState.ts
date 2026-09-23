import { countForm, type CountForm } from '../i18n/countForm.ts'
import type { DocumentTone } from '../documents/status.ts'

/**
 * How far off an instalment is, in words, and in the tone the documents use -
 * so an instalment due tomorrow on Acasă looks exactly as urgent as an RCA
 * expiring tomorrow beside it.
 */
export type PaymentDueKey = 'payments.due.today' | `payments.due.${CountForm}`

export interface PaymentDueState {
  readonly tone: DocumentTone
  readonly key: PaymentDueKey
  readonly values: Record<string, string | number>
}

/**
 * Today and tomorrow are urgent, the rest of the reminder horizon is "soon",
 * and anything further off is simply the next date. The seven matches the
 * longest lead a payment's reminder can have, and the dashboard's horizon.
 */
export function dueStateOf(
  days: number, dueDate: string, formatDate: (iso: string) => string,
): PaymentDueState {
  const date = formatDate(dueDate)

  if (days <= 0) {
    return { tone: 'urgent', key: 'payments.due.today', values: { date } }
  }

  const tone: DocumentTone = days === 1 ? 'urgent' : days <= 7 ? 'soon' : 'ok'
  return { tone, key: `payments.due.${countForm(days)}`, values: { days, date } }
}
