import { describe, expect, it } from 'vitest'

import { dateFormatter, stateOf } from './status.ts'

/** Identity, so the assertions are about the branching rather than about Intl. */
const asIs = (iso: string) => iso

describe('document state', () => {
  it('names the days left while cover holds', () => {
    expect(stateOf({ status: 'ACTIVE', validUntil: '2027-01-01', daysRemaining: 200 }, asIs))
      .toEqual({ tone: 'ok', key: 'documents.state.active', values: { days: 200 } })
  })

  /**
   * Urgency is carried by words as well as by the tone. A screen reader is told
   * nothing by a colour, and the two bands would otherwise read identically.
   */
  it('separates soon from urgent in the sentence, not only in the tone', () => {
    const soon = stateOf({ status: 'EXPIRING_SOON', daysRemaining: 20 }, asIs)
    const urgent = stateOf({ status: 'URGENT', daysRemaining: 3 }, asIs)

    expect(soon.tone).toBe('soon')
    expect(urgent.tone).toBe('urgent')
    expect(soon.key).not.toBe(urgent.key)
  })

  it('counts elapsed days as a positive number', () => {
    expect(stateOf({ status: 'EXPIRED', validUntil: '2026-08-12', daysRemaining: -5 }, asIs))
      .toEqual({ tone: 'gap', key: 'documents.state.lapsed', values: { days: 5 } })
  })

  /**
   * The three situations section 11 hands over as one status. They must not read
   * alike: one is a lapse, one is a lapse with cover already arranged, and one is
   * a policy that has never yet begun.
   */
  it('tells the three EXPIRED situations apart by their dates', () => {
    const lapsed = stateOf({ status: 'EXPIRED', validUntil: '2026-08-12', daysRemaining: -5 }, asIs)
    const arranged = stateOf({
      status: 'EXPIRED', validUntil: '2026-08-12', daysRemaining: -5, upcomingFrom: '2026-09-01',
    }, asIs)
    const notStarted = stateOf({ status: 'EXPIRED', upcomingFrom: '2026-09-01' }, asIs)

    expect(new Set([lapsed.key, arranged.key, notStarted.key]).size).toBe(3)
    expect(arranged.values).toEqual({ days: 5, date: '2026-09-01' })
    expect(notStarted.values).toEqual({ date: '2026-09-01' })
  })

  it('reports a type nothing was ever entered for as unset', () => {
    expect(stateOf({ status: 'NOT_CONFIGURED' }, asIs))
      .toEqual({ tone: 'unset', key: 'documents.state.notConfigured', values: {} })
  })

  it('falls back to the raw value when a date cannot be read', () => {
    expect(dateFormatter('ro')('not-a-date')).toBe('not-a-date')
    expect(dateFormatter('ro')('2026-09-01')).not.toBe('2026-09-01')
  })
})