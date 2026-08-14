import { describe, expect, it } from 'vitest'

import { carriedEmail } from './carriedEmail.ts'

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