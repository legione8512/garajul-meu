import { act, renderHook } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { ApiError } from '../api/ApiError.ts'
import { useSubmission } from './useSubmission.ts'

describe('form submission', () => {
  it('captures an ApiError and stops being pending', async () => {
    const { result } = renderHook(() => useSubmission())

    await act(async () => {
      await result.current.submit(() =>
        Promise.reject(new ApiError('EMAIL_ALREADY_EXISTS', 409, 'r-1', [])))
    })

    expect(result.current.pending).toBe(false)
    expect(result.current.error?.code).toBe('EMAIL_ALREADY_EXISTS')
  })

  it('reports anything that never reached the backend as UNKNOWN', async () => {
    const { result } = renderHook(() => useSubmission())

    await act(async () => {
      await result.current.submit(() => Promise.reject(new Error('offline')))
    })

    expect(result.current.error?.code).toBe('UNKNOWN')
  })
})