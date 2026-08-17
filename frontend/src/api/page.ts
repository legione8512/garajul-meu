/**
 * One page of anything, mirroring the backend's `PageResponse`.
 *
 * <p>The server deliberately does not serialise Spring Data's `Page`, whose JSON
 * shape has moved between releases; this is the stable contract it sends
 * instead, and every paginated endpoint answers with it.
 */
export interface PageResponse<T> {
  readonly items: readonly T[]
  readonly page: number
  readonly size: number
  readonly totalElements: number
  readonly totalPages: number
}