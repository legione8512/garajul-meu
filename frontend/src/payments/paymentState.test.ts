import { describe, expect, it } from 'vitest'

import { dueStateOf } from './paymentState.ts'

const format = (iso: string) => `[${iso}]`

describe('how far off an instalment is', () => {
  it('says today, and marks it urgent', () => {
    expect(dueStateOf(0, '2026-10-15', format))
      .toEqual({ tone: 'urgent', key: 'payments.due.today', values: { date: '[2026-10-15]' } })
  })

  it('marks tomorrow urgent and the rest of the week soon', () => {
    expect(dueStateOf(1, '2026-10-15', format).tone).toBe('urgent')
    expect(dueStateOf(1, '2026-10-15', format).key).toBe('payments.due.one')
    expect(dueStateOf(3, '2026-10-15', format).tone).toBe('soon')
    expect(dueStateOf(7, '2026-10-15', format).tone).toBe('soon')
  })

  it('is only a date further off, and counts in Romanian', () => {
    expect(dueStateOf(8, '2026-10-15', format).tone).toBe('ok')
    expect(dueStateOf(12, '2026-10-15', format).key).toBe('payments.due.few')
    expect(dueStateOf(25, '2026-10-15', format).key).toBe('payments.due.many')
  })
})
