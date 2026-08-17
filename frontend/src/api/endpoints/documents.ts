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
 * The period and its details, without the type. Mirrors the backend's
 * DocumentPeriod, which exists there so section 12's rule about the pair of
 * dates lives in one method rather than two.
 */
export interface DocumentPeriodBody {
  readonly validFrom: string | null
  readonly validUntil: string
  readonly provider: string | null
  readonly referenceNumber: string | null
  readonly notes: string | null
}

/**
 * What adding and correcting send. The type is a plain string because that is
 * what a select yields and what the backend validates by name; narrowing it here
 * would move the cast rather than remove it.
 */
export interface NewDocument extends DocumentPeriodBody {
  readonly type: string
}

export function addDocument(vehicleId: string, document: NewDocument): Promise<DocumentDetails> {
  return apiFetch<DocumentDetails>(documentsPath(vehicleId), {
    method: 'POST',
    body: JSON.stringify(document),
  })
}

/**
 * A correction replaces the record rather than patching it, which is why this
 * sends every field including the ones left empty - an omitted optional means
 * the user cleared it, and the backend reads it exactly that way.
 */
export function correctDocument(
  vehicleId: string, documentId: string, document: NewDocument,
): Promise<DocumentDetails> {
  return apiFetch<DocumentDetails>(documentPath(vehicleId, documentId), {
    method: 'PATCH',
    body: JSON.stringify(document),
  })
}

/**
 * A renewal is a new row and answers as one: the returned document has a new
 * identifier, and the record it supersedes is untouched. No type is sent,
 * because it is taken from the record being renewed - that is what makes this a
 * renewal rather than another document.
 */
export function renewDocument(
  vehicleId: string, documentId: string, period: DocumentPeriodBody,
): Promise<DocumentDetails> {
  return apiFetch<DocumentDetails>(`${documentPath(vehicleId, documentId)}/renew`, {
    method: 'POST',
    body: JSON.stringify(period),
  })
}

export function deleteDocument(vehicleId: string, documentId: string): Promise<void> {
  return apiFetch<void>(documentPath(vehicleId, documentId), { method: 'DELETE' })
}

/**
 * The chronological history of one vehicle's documents, section 16's paginated
 * endpoint.
 *
 * <p>The filter and the page live in the path rather than in component state
 * because `useResource` keys its effect on the path string: changing either
 * changes the string, and the screen reloads without any further machinery.
 * Absent parameters are omitted rather than sent empty, so the default page has
 * a clean address.
 */
export function historyPath(
  vehicleId: string, options: { readonly type?: string, readonly page?: number } = {},
): string {
  const query = new URLSearchParams()

  if (options.type !== undefined && options.type !== '') {
    query.set('type', options.type)
  }
  if (options.page !== undefined && options.page > 0) {
    query.set('page', String(options.page))
  }

  const suffix = query.toString()
  return `/api/v1/vehicles/${vehicleId}/history${suffix === '' ? '' : `?${suffix}`}`
}