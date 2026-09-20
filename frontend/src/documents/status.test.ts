import { describe, expect, it, vi } from 'vitest'

import { dateFormatter, stateOf } from './status.ts'

/** Identity, so the assertions are about the branching rather than about Intl. */
const asIs = (iso: string) => iso

describe('document state', () => {
  it('names the days left while cover holds', () => {
    expect(stateOf({ status: 'ACTIVE', validUntil: '2027-01-01', daysRemaining: 200 }, asIs))
      .toEqual({ tone: 'ok', key: 'documents.state.active.many', values: { days: 200 } })
  })

  /**
   * The Play screenshots of 2026-09-19 read "Expiră în 1 zile" and "Valabil
   * încă 223 zile". Each count now picks the sentence that agrees with it.
   */
  it('chooses the sentence whose form agrees with the count', () => {
    const key = (status: 'ACTIVE' | 'EXPIRING_SOON' | 'URGENT', daysRemaining: number) =>
      stateOf({ status, daysRemaining }, asIs).key

    expect(key('URGENT', 1)).toBe('documents.state.urgent.one')
    expect(key('URGENT', 6)).toBe('documents.state.urgent.few')
    expect(key('EXPIRING_SOON', 19)).toBe('documents.state.soon.few')
    expect(key('EXPIRING_SOON', 20)).toBe('documents.state.soon.many')
    expect(key('ACTIVE', 101)).toBe('documents.state.active.few')
    expect(key('ACTIVE', 223)).toBe('documents.state.active.many')
  })

  it('says yesterday rather than one day ago', () => {
    expect(stateOf({ status: 'EXPIRED', validUntil: '2026-09-18', daysRemaining: -1 }, asIs).key)
      .toBe('documents.state.lapsed.one')
    expect(stateOf({
      status: 'EXPIRED', validUntil: '2026-09-18', daysRemaining: -1, upcomingFrom: '2026-10-01',
    }, asIs).key).toBe('documents.state.lapsedUntil.one')
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
      .toEqual({ tone: 'gap', key: 'documents.state.lapsed.few', values: { days: 5 } })
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

  /**
   * The demo account's RCA of 2026-09-15: running from 5 December 2026 to
   * 4 December 2027, and read on 15 September 2026 as "valid for another 445
   * days". The period holds 364 days and 445 remain, so it has not begun.
   */
  it('says when a stored document takes effect, rather than that it is valid', () => {
    expect(stateOf({
      status: 'ACTIVE', validFrom: '2026-12-05', validUntil: '2027-12-04', daysRemaining: 445,
    }, asIs)).toEqual({ tone: 'ok', key: 'documents.state.notStarted', values: { date: '2026-12-05' } })
  })

  /** Measured at both edges: the day before it starts, and the day it starts. */
  it('reads a document as active from the morning it starts, and not before', () => {
    const period = { validFrom: '2026-12-05', validUntil: '2027-12-04' } as const

    expect(stateOf({ status: 'ACTIVE', ...period, daysRemaining: 365 }, asIs).key)
      .toBe('documents.state.notStarted')
    expect(stateOf({ status: 'ACTIVE', ...period, daysRemaining: 364 }, asIs).key)
      .toBe('documents.state.active.many')
  })

  /** A short document can start tomorrow and still sit in an urgent band. */
  it('applies whatever band the expiry falls in', () => {
    expect(stateOf({
      status: 'URGENT', validFrom: '2026-09-17', validUntil: '2026-09-19', daysRemaining: 4,
    }, asIs).key).toBe('documents.state.notStarted')
  })

  it('treats a document with no start date as started', () => {
    expect(stateOf({ status: 'ACTIVE', validUntil: '2027-12-04', daysRemaining: 445 }, asIs).key)
      .toBe('documents.state.active.many')
  })

  it('reports a type nothing was ever entered for as unset', () => {
    expect(stateOf({ status: 'NOT_CONFIGURED' }, asIs))
      .toEqual({ tone: 'unset', key: 'documents.state.notConfigured', values: {} })
  })

  it('falls back to the raw value when a date cannot be read', () => {
    expect(dateFormatter('ro')('not-a-date')).toBe('not-a-date')
  })

  /**
   * "12/5/2026" was 5 December in US order and read as 12 May (2026-09-15). The
   * month is a word in both languages, and English puts the day first.
   */
  it('writes the month as a word, day first, in both languages', () => {
    expect(dateFormatter('ro')('2026-12-05')).toBe('5 decembrie 2026')
    expect(dateFormatter('en')('2026-12-05')).toBe('5 December 2026')
  })

  /**
   * A date-only string is UTC midnight, and in a timezone west of Greenwich that
   * instant is still the previous evening. Node applies a change to `TZ` at
   * runtime, which is what lets `vi.stubEnv` run the formatter in Los Angeles.
   */
  it('keeps the day west of Greenwich', () => {
    vi.stubEnv('TZ', 'America/Los_Angeles')

    try {
      expect(dateFormatter('ro')('2026-12-05')).toBe('5 decembrie 2026')
    }
    finally {
      vi.unstubAllEnvs()
    }
  })
})