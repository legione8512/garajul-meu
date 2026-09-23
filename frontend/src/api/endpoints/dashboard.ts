import type { DocumentStatus, DocumentType } from './documents.ts'
import type { PaymentKind } from './payments.ts'

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

/**
 * An instalment due within seven days, today included (1.1). The backend
 * decides the horizon; this side shows whatever lines arrive, soonest first.
 */
export interface PaymentDueLine {
  readonly kind: PaymentKind
  readonly paymentId: string
  readonly dueDate: string
  readonly daysRemaining: number
  readonly instalment: number
  readonly instalments: number
}

export interface DashboardVehicle {
  readonly vehicleId: string
  readonly displayName?: string | null
  readonly registrationNumber: string
  readonly make: string
  readonly commercialDescription: string
  /**
   * Whether the card should ask for a thumbnail (1.0.2). The garage's field,
   * meaning the same thing - see VehicleSummary, where the reasoning is.
   */
  readonly hasImage: boolean
  readonly documents: readonly DocumentStatusLine[]
  /**
   * Since 1.1. Optional so that a backend answering without it - one not yet
   * redeployed - reads as "nothing due" rather than breaking the card.
   */
  readonly payments?: readonly PaymentDueLine[]
}

export interface DashboardView {
  readonly vehicles: readonly DashboardVehicle[]
}