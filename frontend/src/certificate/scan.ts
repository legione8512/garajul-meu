import type { OcrScan, ScanStatus } from '../api/endpoints/ocr.ts'
import { certificateFields, type CertificateField } from './fields.ts'
import type { CertificateForm } from './values.ts'

export type FieldStatuses = Partial<Record<CertificateField, ScanStatus>>

export interface ScanResult {
  readonly form: CertificateForm
  readonly statuses: FieldStatuses
}

export interface ScanTally {
  readonly detected: number
  readonly needsReview: number
  readonly notDetected: number
}

const knownFields = new Set<string>(certificateFields.map(field => field.name))

function isCertificateField(name: string): name is CertificateField {
  return knownFields.has(name)
}

/**
 * Lays a scan over the form the person is already looking at.
 *
 * <p><strong>A proposal with no value leaves what is there alone.</strong> Not
 * detected has to mean "the photograph did not show me this", never "empty it" -
 * somebody who typed their VIN by hand and then scanned a creased certificate
 * must not watch it disappear. Only a proposal that actually carries a value
 * writes one.
 *
 * <p>Values are taken exactly as they arrive. The backend proposes only what the
 * field can hold, so there is nothing to convert here, and converting anyway
 * would be a second place for the two sides to disagree.
 *
 * <p>A field name this build does not know is skipped rather than written, so a
 * backend one version ahead cannot put a key into the form that no input reads
 * and no save would send.
 */
export function applyScan(form: CertificateForm, scan: OcrScan): ScanResult {
  const next = { ...form }
  const statuses: FieldStatuses = {}

  for (const proposal of scan.fields) {
    if (!isCertificateField(proposal.field)) {
      continue
    }

    statuses[proposal.field] = proposal.status

    if (proposal.value !== null) {
      next[proposal.field] = proposal.value
    }
  }

  return { form: next, statuses }
}

/** What the screen tells the person after a scan, counted once. */
export function tally(statuses: FieldStatuses): ScanTally {
  const values = Object.values(statuses)

  return {
    detected: values.filter(status => status === 'DETECTED').length,
    needsReview: values.filter(status => status === 'NEEDS_REVIEW').length,
    notDetected: values.filter(status => status === 'NOT_DETECTED').length,
  }
}