import type { ReminderStatus, ReminderView } from '../api/endpoints/reminders.ts'
import type { DocumentTone } from './status.ts'

/**
 * How far ahead a reminder fires, as four literal keys.
 *
 * <p><strong>Romanian counts differently above nineteen</strong>: "cu 14 zile
 * înainte", but "cu 30 de zile înainte". That is the language's rule and not
 * this offset set's, so the split is written generally - adding a sixty-day
 * offset later must not produce "cu 60 zile", which reads as a mistake to every
 * Romanian speaker and to no test. English needs no such split and gets the same
 * sentence twice, which is cheaper than a second mechanism.
 *
 * <p>i18next could do this with CLDR plurals, and Romanian's few/other boundary
 * is exactly where these keys divide. It is not used because `t` is typed over
 * the literal keys of the locale object, and a plural's base key -
 * `reminders.lead` - exists in none of them. The pattern here is the one
 * status.ts already proved compiles.
 */
export type ReminderLeadKey =
  | 'reminders.lead.onTheDay'
  | 'reminders.lead.oneDay'
  | 'reminders.lead.fewDays'
  | 'reminders.lead.manyDays'

/** What became of it. One key per status, and the locale holds exactly five. */
export type ReminderOutcomeKey =
  | 'reminders.outcome.scheduled'
  | 'reminders.outcome.sending'
  | 'reminders.outcome.sent'
  | 'reminders.outcome.failed'
  | 'reminders.outcome.cancelled'

/**
 * Two sentences and a tone. A type alias rather than an interface, because both
 * value bags are handed straight to i18next.
 *
 * <p>The tone vocabulary is DocumentTone's rather than one of its own: the two
 * live on the same screen, and a design system that eventually styles
 * `data-tone` should have one set of names to style rather than two.
 */
export type ReminderLine = {
  readonly leadKey: ReminderLeadKey
  readonly leadValues: Record<string, string | number>
  readonly outcomeKey: ReminderOutcomeKey
  readonly outcomeValues: Record<string, string | number>
  readonly tone: DocumentTone
}

export function leadKeyFor(offsetDays: number): ReminderLeadKey {
  if (offsetDays === 0) {
    return 'reminders.lead.onTheDay'
  }
  if (offsetDays === 1) {
    return 'reminders.lead.oneDay'
  }
  return offsetDays < 20 ? 'reminders.lead.fewDays' : 'reminders.lead.manyDays'
}

/**
 * The line one reminder deserves.
 *
 * <p><strong>A sent reminder reports when it was sent, not when it was due.</strong>
 * The two are usually a second apart and occasionally not: a reminder the
 * scheduler picked up late, or retried, completed at an instant its schedule
 * never mentioned, and the reader is owed the one that happened.
 *
 * <p>All five statuses are listed and there is no default. A sixth would stop
 * this compiling, which is the intent - falling through to "scheduled" would put
 * a confident sentence under a state nobody had thought about.
 *
 * <p>The formatter is passed in rather than reached for, as in status.ts: a pure
 * function that needed a hook could not be tested without one.
 */
export function lineOf(
  reminder: ReminderView, formatDateTime: (iso: string) => string,
): ReminderLine {
  const lead = {
    leadKey: leadKeyFor(reminder.offsetDays),
    leadValues: { days: reminder.offsetDays },
  }

  switch (reminder.status) {
    case 'PENDING':
      return {
        ...lead,
        outcomeKey: 'reminders.outcome.scheduled',
        outcomeValues: { when: formatDateTime(reminder.scheduledAt) },
        tone: 'unset',
      }
    case 'PROCESSING':
      return {
        ...lead,
        outcomeKey: 'reminders.outcome.sending',
        outcomeValues: {},
        tone: 'unset',
      }
    case 'SENT':
      return {
        ...lead,
        outcomeKey: 'reminders.outcome.sent',
        outcomeValues: { when: formatDateTime(reminder.sentAt ?? reminder.scheduledAt) },
        tone: 'ok',
      }
    case 'FAILED':
      return {
        ...lead,
        outcomeKey: 'reminders.outcome.failed',
        outcomeValues: {},
        tone: 'gap',
      }
    case 'CANCELLED':
      return {
        ...lead,
        outcomeKey: 'reminders.outcome.cancelled',
        outcomeValues: {},
        tone: 'unset',
      }
  }
}

/**
 * A date and a time as the reader writes them. The time is the point: nine in
 * the morning is what the whole schedule is arranged around, and a date alone
 * would hide it.
 *
 * <p>Falls back to what arrived rather than throwing, as dateFormatter does.
 */
export function dateTimeFormatter(language: string): (iso: string) => string {
  return (iso: string) => {
    const parsed = new Date(iso)

    if (Number.isNaN(parsed.getTime())) {
      return iso
    }
    return parsed.toLocaleString(language, { dateStyle: 'medium', timeStyle: 'short' })
  }
}

/** Exported for the test that checks every status produces wording that exists. */
export const reminderStatuses: readonly ReminderStatus[] =
  ['PENDING', 'PROCESSING', 'SENT', 'FAILED', 'CANCELLED']