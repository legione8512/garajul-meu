import { apiFetch } from '../client.ts'

/**
 * The vehicle-document surface of the API.
 *
 * <p>Reads are paths and writes are functions, for the reason vehicles.ts gives:
 * useResource keys its effect on a string, and a string is stable between
 * renders where a function is not.
 */
export function documentsPath(vehicleId: string): string {
  return `/api/v1/vehicles/${vehicleId}/documents`
}

export function documentPath(vehicleId: string, documentId: string): string {
  return `${documentsPath(vehicleId)}/${documentId}`
}

/** The four types section 1 names. Sent as text; the backend refuses anything else by name. */
export const documentTypes = ['RCA', 'CASCO', 'ITP', 'ROVINIETA'] as const

export type DocumentType = (typeof documentTypes)[number]

/**
 * Section 11's six states. NOT_CONFIGURED never arrives on a stored document -
 * it is what the dashboard reports for a type nothing was ever entered for -
 * but it belongs in the union because one function reads both shapes.
 */
export type DocumentStatus =
  | 'ACTIVE'
  | 'EXPIRING_SOON'
  | 'URGENT'
  | 'EXPIRES_TODAY'
  | 'EXPIRED'
  | 'NOT_CONFIGURED'

/**
 * One stored document. `status` and `daysRemaining` are computed by the backend
 * for the reader's timezone and are not columns - section 11 defines both as
 * statements about a document *and a date*, so they change overnight without
 * anything being written.
 */
export interface DocumentDetails {
  readonly id: string
  readonly type: DocumentType
  readonly validFrom?: string | null
  readonly validUntil: string
  readonly provider?: string | null
  readonly referenceNumber?: string | null
  readonly notes?: string | null
  readonly status: DocumentStatus
  readonly daysRemaining: number
}

/**
 * What the form sends. The type is a plain string because that is what a select
 * yields and what the backend validates; narrowing it here would only move the
 * cast, not remove it.
 */
export interface NewDocument {
  readonly type: string
  readonly validFrom: string | null
  readonly validUntil: string
  readonly provider: string | null
  readonly referenceNumber: string | null
  readonly notes: string | null
}

export function addDocument(vehicleId: string, document: NewDocument): Promise<DocumentDetails> {
  return apiFetch<DocumentDetails>(documentsPath(vehicleId), {
    method: 'POST',
    body: JSON.stringify(document),
  })
}

export function deleteDocument(vehicleId: string, documentId: string): Promise<void> {
  return apiFetch<void>(documentPath(vehicleId, documentId), { method: 'DELETE' })
}