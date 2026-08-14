import { useCallback, useState } from 'react'

import { ApiError } from '../api/ApiError.ts'

export interface Submission {
  pending: boolean
  error: ApiError | null
  /** Null when the action succeeded; otherwise the failure, for the caller to inspect. */
  submit: (action: () => Promise<void>) => Promise<ApiError | null>
  reset: () => void
}

/**
 * Runs one form submission and remembers how it went, so five screens do not
 * each reimplement pending state and error capture.
 *
 * <p>It returns the failure rather than a boolean because the caller needs it:
 * field errors have to be mapped onto inputs immediately, and the `error` state
 * below has not been applied yet inside the closure that just called submit.
 */
export function useSubmission(): Submission {
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<ApiError | null>(null)

  const submit = useCallback(async (action: () => Promise<void>) => {
    setPending(true)
    setError(null)

    try {
      await action()
      return null
    } catch (thrown) {
      // Anything that is not an ApiError never reached the backend - the
      // network failed, or we have a bug. UNKNOWN says exactly that; inventing
      // a backend code would claim something the server never said.
      const failure = thrown instanceof ApiError ? thrown : new ApiError('UNKNOWN', 0, null, [])
      setError(failure)
      return failure
    } finally {
      setPending(false)
    }
  }, [])

  const reset = useCallback(() => {
    setError(null)
  }, [])

  return { pending, error, submit, reset }
}