import { describe, expect, it } from 'vitest'

import type { ReminderView } from '../api/endpoints/reminders.ts'
import { ro } from '../i18n/locales/ro.ts'
import { dateTimeFormatter, leadKeyFor, lineOf, reminderStatuses } from './reminderState.ts'

const SCHEDULED = '2026-11-01T07:00:00Z'
const COMPLETED = '2026-11-01T07:04:00Z'

const PENDING: ReminderView = {
  offsetDays: 30,
  scheduledAt: SCHEDULED,
  status: 'PENDING',
  sentAt: null,
}

/** Fixed wording, so a test asserts about the line and not about a locale. */
const format = (iso: string) => `<${iso}>`

describe('reminder lines', () => {
  /**
   * The rule that has nothing to do with software. Thirty is the only default
   * offset that needs "de", which is exactly why it is asserted beside ones that
   * do not - the general rule is what is being tested, not the one case.
   */
  it('splits where Romanian splits, above nineteen', () => {
    expect(leadKeyFor(30)).toBe('reminders.lead.manyDays')
    expect(leadKeyFor(20)).toBe('reminders.lead.manyDays')
    expect(leadKeyFor(19)).toBe('reminders.lead.fewDays')
    expect(leadKeyFor(14)).toBe('reminders.lead.fewDays')
    expect(leadKeyFor(3)).toBe('reminders.lead.fewDays')
  })

  it('says one day and the day itself in words rather than numbers', () => {
    expect(leadKeyFor(1)).toBe('reminders.lead.oneDay')
    expect(leadKeyFor(0)).toBe('reminders.lead.onTheDay')
  })

  it('reports a pending reminder by when it will fire', () => {
    const line = lineOf(PENDING, format)

    expect(line.outcomeKey).toBe('reminders.outcome.scheduled')
    expect(line.outcomeValues.when).toBe(`<${SCHEDULED}>`)
    expect(line.tone).toBe('unset')
  })

  /**
   * The two instants differ whenever a reminder was picked up late or retried,
   * and the reader is owed the one that happened rather than the one that was
   * planned. Asserted against both so a copy-paste between them would fail.
   */
  it('reports a sent reminder by when it was sent, not when it was due', () => {
    const line = lineOf({ ...PENDING, status: 'SENT', sentAt: COMPLETED }, format)

    expect(line.outcomeKey).toBe('reminders.outcome.sent')
    expect(line.outcomeValues.when).toBe(`<${COMPLETED}>`)
    expect(line.outcomeValues.when).not.toBe(`<${SCHEDULED}>`)
    expect(line.tone).toBe('ok')
  })

  /** A reminder recorded as sent without an instant still has a sentence. */
  it('falls back to the schedule when a sent reminder carries no instant', () => {
    const line = lineOf({ ...PENDING, status: 'SENT', sentAt: null }, format)

    expect(line.outcomeValues.when).toBe(`<${SCHEDULED}>`)
  })

  it('says a failure plainly and promises no instant for it', () => {
    const line = lineOf({ ...PENDING, status: 'FAILED' }, format)

    expect(line.outcomeKey).toBe('reminders.outcome.failed')
    expect(line.outcomeValues).toEqual({})
    expect(line.tone).toBe('gap')
  })

  /**
   * Both directions, and the reason this test exists twice over. Checking only
   * that produced keys have wording lets a translated key go unproduced, and a
   * key nothing produces is wording nobody is checking - which is how Phase 9's
   * five OCR codes sat outside their guard for two whole phases while appearing,
   * translated, on real screens.
   */
  it('produces exactly the wording the locale holds, and no more', () => {
    const outcomes = reminderStatuses.map(
      status => lineOf({ ...PENDING, status }, format).outcomeKey,
    )

    for (const key of outcomes) {
      const name = key.replace('reminders.outcome.', '')
      expect(ro.reminders.outcome[name as keyof typeof ro.reminders.outcome], key).toBeTruthy()
    }
    expect(new Set(outcomes).size).toBe(Object.keys(ro.reminders.outcome).length)

    const leads = [0, 1, 3, 30].map(offset => leadKeyFor(offset))

    for (const key of leads) {
      const name = key.replace('reminders.lead.', '')
      expect(ro.reminders.lead[name as keyof typeof ro.reminders.lead], key).toBeTruthy()
    }
    expect(new Set(leads).size).toBe(Object.keys(ro.reminders.lead).length)
  })

  /** A malformed instant is a bad line on a screen, never a blank one. */
  it('renders what arrived when it is not a date at all', () => {
    expect(dateTimeFormatter('ro')('not-a-date')).toBe('not-a-date')
  })
})