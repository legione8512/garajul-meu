export interface FieldError {
  readonly field: string
  readonly constraint: string
}

/**
 * A failed request, in the shape the backend actually sends.
 *
 * <p>There is no message from the server and never will be - section 17 gives a
 * failure one stable code and nothing else. The Error message here exists for
 * developers reading a stack trace; anything a user reads comes from
 * errorMessageKey and i18next.
 */
export class ApiError extends Error {
  readonly code: string

  readonly status: number

  /** The correlation id, when the server sent one. Quoted on error screens. */
  readonly requestId: string | null

  readonly fieldErrors: readonly FieldError[]

  constructor(code: string, status: number, requestId: string | null, fieldErrors: readonly FieldError[]) {
    super(`API request failed with ${code} (${String(status)})`)
    this.name = 'ApiError'
    this.code = code
    this.status = status
    this.requestId = requestId
    this.fieldErrors = fieldErrors
  }
}