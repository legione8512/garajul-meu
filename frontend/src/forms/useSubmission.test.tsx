import { act, renderHook } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { ApiError } from '../api/ApiError.ts'
import { useSubmission } from './useSubmission.ts'

describe('form submission', () => {
  it('captures an ApiError, returns it, and stops being pending', async () => {
    const { result } = renderHook(() => useSubmission())

    // Taken from act's own return rather than assigned to an outer variable:
    // TypeScript does not follow assignments made inside a callback, so the
    // variable would still be typed as its initial value afterwards.
    const returned = await act(() => result.current.submit(() =>
      Promise.reject(new ApiError('EMAIL_ALREADY_EXISTS', 409, 'r-1', []))))

    expect(result.current.pending).toBe(false)
    expect(result.current.error?.code).toBe('EMAIL_ALREADY_EXISTS')
    expect(returned?.code).toBe('EMAIL_ALREADY_EXISTS')
  })

  it('returns null when the action succeeds', async () => {
    const { result } = renderHook(() => useSubmission())

    const returned = await act(() => result.current.submit(() => Promise.resolve()))

    expect(returned).toBeNull()
    expect(result.current.error).toBeNull()
  })

  it('reports anything that never reached the backend as UNKNOWN', async () => {
    const { result } = renderHook(() => useSubmission())

    await act(async () => {
      await result.current.submit(() => Promise.reject(new Error('offline')))
    })

    expect(result.current.error?.code).toBe('UNKNOWN')
  })
})