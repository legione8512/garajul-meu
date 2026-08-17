import type { DocumentStatus, DocumentType } from './documents.ts'

export const dashboardPath = '/api/v1/dashboard'

/**
 * One type of document for one vehicle, as of the reader's today.
 *
 * <p>All four types arrive for every vehicle, including the ones nothing was
 * ever entered for - which of them a screen shows is the screen's decision. The
 * nullable fields explain the status rather than change it: no `documentId` at
 * all means never configured; `upcomingFrom` without `validUntil` is a policy
 * bought and not yet started; both together are a lapse with cover already
 * arranged.
 */
export interface DocumentStatusLine {
  readonly type: DocumentType
  readonly status: DocumentStatus
  readonly documentId?: string | null
  readonly validUntil?: string | null
  readonly daysRemaining?: number | null
  readonly upcomingFrom?: string | null
}

export interface DashboardVehicle {
  readonly vehicleId: string
  readonly displayName?: string | null
  readonly registrationNumber: string
  readonly make: string
  readonly commercialDescription: string
  readonly documents: readonly DocumentStatusLine[]
}

export interface DashboardView {
  readonly vehicles: readonly DashboardVehicle[]
}