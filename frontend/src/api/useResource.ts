import { useCallback, useEffect, useState } from 'react'

import { ApiError } from './ApiError.ts'
import { apiFetch } from './client.ts'

export interface Resource<T> {
  readonly data: T | null
  readonly error: ApiError | null
  readonly loading: boolean
  readonly reload: () => void
}

interface Settled<T> {
  readonly key: string
  readonly data: T | null
  readonly error: ApiError | null
}

/**
 * Reads one address and reports the three states a screen has to draw.
 *
 * <p>Section 3 lists the frontend stack without a data-fetching library, so this
 * is hand-written for the same reason the forms are: a handful of screens with
 * one GET each do not carry TanStack Query's weight.
 *
 * <p><strong>Phase 10 was the review this used to schedule, and the answer was
 * no.</strong> The note here said that deleting a document would have to refresh
 * a list on another screen, and that a cache solves that where this does not.
 * Phase 10 shipped screens 11, 13, 14 and the dashboard, and nothing went stale
 * - because there is no cache to go stale. Every mount fetches, so a screen
 * arrived at after a change anywhere always shows the change. The cost is a
 * request per mount, and the invalidation problem only exists once something is
 * kept. Revisit if a screen ever has to show data changed by another screen
 * **without a navigation between them**, which is a different problem and the
 * only one a cache would be earning its keep against.
 *
 * <p><strong>The dependency is a path, not a function.</strong> A hook taking
 * {@code () => apiFetch(...)} re-runs on every render, because the caller builds
 * a new function each time and the effect sees a changed dependency - the screen
 * then reloads itself forever. A string cannot do that, and it is also what lets
 * a filter and a page number live in the address: screen 14 changes either and
 * the reload needs no further wiring.
 *
 * <p><strong>Loading is derived, not stored.</strong> Setting a flag at the top
 * of the effect would re-render the component before the request had even been
 * sent, which is what react-hooks/set-state-in-effect objects to. Instead the
 * one piece of state records which request it answers, and anything whose key
 * does not match the current one is simply not this screen's answer. The state
 * is only ever written from the settled promise, which is asynchronous and
 * therefore fine.
 *
 * <p>A failure that is not an ApiError becomes UNKNOWN rather than being stored
 * as it is. fetch rejects with a TypeError when the connection drops, which has
 * no code at all, and a screen reading error.code would render nothing while
 * something was plainly wrong.
 */
export function useResource<T>(path: string): Resource<T> {
  const [attempt, setAttempt] = useState(0)
  const [settled, setSettled] = useState<Settled<T>>({ key: '', data: null, error: null })

  const key = `${path}#${String(attempt)}`

  const reload = useCallback(() => {
    setAttempt(previous => previous + 1)
  }, [])

  useEffect(() => {
    // Guards against a response arriving after the screen has been left. The
    // request cannot be recalled, so the answer is to ignore its answer rather
    // than write it into a component that is no longer mounted.
    let current = true

    apiFetch<T>(path)
      .then(result => {
        if (current) {
          setSettled({ key, data: result, error: null })
        }
      })
      .catch((cause: unknown) => {
        if (current) {
          setSettled({
            key,
            data: null,
            error: cause instanceof ApiError ? cause : new ApiError('UNKNOWN', 0, null, []),
          })
        }
      })

    return () => {
      current = false
    }
  }, [key, path])

  const answered = settled.key === key

  return {
    data: answered ? settled.data : null,
    error: answered ? settled.error : null,
    loading: !answered,
    reload,
  }
}