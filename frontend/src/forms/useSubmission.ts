import { useCallback, useState } from 'react'

import { ApiError } from '../api/ApiError.ts'

export interface Submission {
  pending: boolean
  error: ApiError | null
  submit: (action: () => Promise<void>) => Promise<boolean>
  reset: () => void
}

/**
 * Runs one form submission and remembers how it went, so five screens do not
 * each reimplement pending state and error capture.
 */
export function useSubmission(): Submission {
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<ApiError | null>(null)

  const submit = useCallback(async (action: () => Promise<void>) => {
    setPending(true)
    setError(null)

    try {
      await action()
      return true
    } catch (thrown) {
      // Anything that is not an ApiError never reached the backend - the
      // network failed, or we have a bug. UNKNOWN says exactly that; inventing
      // a backend code would claim something the server never said.
      setError(thrown instanceof ApiError ? thrown : new ApiError('UNKNOWN', 0, null, []))
      return false
    } finally {
      setPending(false)
    }
  }, [])

  const reset = useCallback(() => {
    setError(null)
  }, [])

  return { pending, error, submit, reset }
}