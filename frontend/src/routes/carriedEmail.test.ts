import { describe, expect, it } from 'vitest'

import { carriedEmail, returnTo } from './carriedEmail.ts'
describe('address handed over between screens', () => {
  it('reads the address a previous screen supplied', () => {
    expect(carriedEmail({ email: 'marius@example.com' })).toBe('marius@example.com')
  })

  /** What a reload leaves behind, and every screen has to survive it. */
  it('answers with nothing when there is no state at all', () => {
    expect(carriedEmail(null)).toBe('')
    expect(carriedEmail(undefined)).toBe('')
  })

  it('answers with nothing when the value is not a string', () => {
    expect(carriedEmail({ email: 42 })).toBe('')
    expect(carriedEmail({ somethingElse: 'marius@example.com' })).toBe('')
  })
})
describe('address a protected route turned somebody away from', () => {
  it('reads a path that was recorded', () => {
    expect(returnTo({ from: '/garage' })).toBe('/garage')
  })

  it('answers with nothing when there is no such state', () => {
    expect(returnTo(null)).toBeNull()
    expect(returnTo({ email: 'marius@example.com' })).toBeNull()
  })

  /**
   * The check that stops this becoming an open redirect: anything able to place
   * a value in route state could otherwise send a freshly signed-in person to
   * another origin.
   */
  it('refuses anything that is not a path on this site', () => {
    expect(returnTo({ from: 'https://elsewhere.example.com/steal' })).toBeNull()
    expect(returnTo({ from: 'javascript:alert(1)' })).toBeNull()
  })
})