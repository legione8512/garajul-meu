import { apiFetch } from '../client.ts'

/** What a message from the Sugestii tab is about (1.1). */
export const feedbackCategories = ['IDEA', 'PROBLEM', 'FEATURE'] as const
export type FeedbackCategory = (typeof feedbackCategories)[number]

/** The backend's limit on one message's length. */
export const FEEDBACK_MAX_LENGTH = 2000

export interface NewFeedback {
  readonly category: string
  readonly message: string
  /** WEB, ANDROID or IOS. */
  readonly platform: string
  /** The installed application's version; null on the web, which has none. */
  readonly appVersion: string | null
}

/**
 * Stored, then emailed to the operator with the account's address to reply to.
 * Answers 204; five in twenty-four hours answers FEEDBACK_LIMIT_REACHED.
 */
export function sendFeedback(feedback: NewFeedback): Promise<void> {
  return apiFetch<void>('/api/v1/feedback', {
    method: 'POST',
    body: JSON.stringify(feedback),
  })
}
