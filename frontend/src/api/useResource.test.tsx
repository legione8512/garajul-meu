import { act, renderHook, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import { useResource } from './useResource.ts'

const PATH = '/api/v1/vehicles'

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('useResource', () => {
  it('answers with the data once the request settles', async () => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.resolve(jsonResponse(200, [{ id: '1' }]))))

    const { result } = renderHook(() => useResource<{ id: string }[]>(PATH))

    expect(result.current.loading).toBe(true)

    await waitFor(() => {
      expect(result.current.data).toEqual([{ id: '1' }])
    })
    expect(result.current.loading).toBe(false)
    expect(result.current.error).toBeNull()
  })

  /** 404 rather than 401, so no refresh is attempted and the code arrives intact. */
  it('reports a refused request as its code instead of throwing', async () => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.resolve(
      jsonResponse(404, { code: 'VEHICLE_NOT_FOUND' }),
    )))

    const { result } = renderHook(() => useResource(PATH))

    await waitFor(() => {
      expect(result.current.error?.code).toBe('VEHICLE_NOT_FOUND')
    })
    expect(result.current.data).toBeNull()
  })

  /**
   * The branch that matters. A dropped connection makes fetch reject with a
   * TypeError, which carries no code - stored as it is, a screen reading
   * error.code would render an empty message while something was plainly wrong.
   */
  it('turns a failure that is not an ApiError into UNKNOWN', async () => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.reject(new TypeError('Failed to fetch'))))

    const { result } = renderHook(() => useResource(PATH))

    await waitFor(() => {
      expect(result.current.error?.code).toBe('UNKNOWN')
    })
  })

  it('reloading asks again', async () => {
    const stub = vi.fn(() => Promise.resolve(jsonResponse(200, [])))
    vi.stubGlobal('fetch', stub)

    const { result } = renderHook(() => useResource(PATH))
    await waitFor(() => {
      expect(result.current.loading).toBe(false)
    })
    expect(stub).toHaveBeenCalledTimes(1)

    act(() => {
      result.current.reload()
    })

    await waitFor(() => {
      expect(stub).toHaveBeenCalledTimes(2)
    })
  })
})