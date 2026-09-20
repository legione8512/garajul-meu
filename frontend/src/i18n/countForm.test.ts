import { describe, expect, it } from 'vitest'

import { countForm } from './countForm.ts'

describe('the Romanian form of a count', () => {
  it('gives one its own form', () => {
    expect(countForm(1)).toBe('one')
  })

  it('writes zero and two to nineteen without "de"', () => {
    for (const count of [0, 2, 7, 14, 19]) {
      expect(countForm(count), String(count)).toBe('few')
    }
  })

  it('writes twenty and above with "de"', () => {
    for (const count of [20, 21, 30, 99, 223]) {
      expect(countForm(count), String(count)).toBe('many')
    }
  })

  /**
   * The rule reads the last two digits, not the size - which is exactly where
   * a plain "below twenty" test goes wrong, at 101 to 119.
   */
  it('drops "de" again after each hundred, and only up to nineteen', () => {
    expect(countForm(100)).toBe('many')
    expect(countForm(101)).toBe('few')
    expect(countForm(119)).toBe('few')
    expect(countForm(120)).toBe('many')
    expect(countForm(1001)).toBe('few')
  })

  it('counts elapsed days, which arrive negative, by their size', () => {
    expect(countForm(-1)).toBe('one')
    expect(countForm(-5)).toBe('few')
    expect(countForm(-223)).toBe('many')
  })

  /** Checked against the language's own rules rather than against a memory of them. */
  it('agrees with CLDR for every count up to 250', () => {
    const rules = new Intl.PluralRules('ro')
    const expected: Record<string, string> = { one: 'one', few: 'few', other: 'many' }

    for (let count = 0; count <= 250; count++) {
      expect(countForm(count), String(count)).toBe(expected[rules.select(count)])
    }
  })
})
