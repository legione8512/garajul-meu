import type { OcrScan, ScanStatus } from '../api/endpoints/ocr.ts'
import { certificateFields, type CertificateField } from './fields.ts'
import type { CertificateForm } from './values.ts'

export type FieldStatuses = Partial<Record<CertificateField, ScanStatus>>

export interface ProposalResult<T> {
  readonly values: T
  readonly statuses: Partial<Record<keyof T & string, ScanStatus>>
}

export interface ScanResult {
  readonly form: CertificateForm
  readonly statuses: FieldStatuses
}

/**
 * A type alias rather than an interface, and the difference is load-bearing:
 * i18next's interpolation parameter requires an index signature, TypeScript
 * gives object type aliases an implicit one, and it refuses interfaces the same
 * courtesy because declaration merging could add keys later. Declared as an
 * interface, `t('certificate.scan.result', counts)` does not compile.
 */
export type ScanTally = {
  readonly detected: number
  readonly needsReview: number
  readonly notDetected: number
}

const certificateFieldNames: readonly CertificateField[] = certificateFields.map(field => field.name)

/**
 * Lays a scan over whatever set of fields a screen actually holds.
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
 * <p>The accepted list is a parameter rather than the field table because two
 * screens take scans and they hold different things: the certificate holds all
 * thirty-two fields, Add Vehicle holds four of them and a nickname that is not
 * on the document at all. A proposal for anything not listed - including a field
 * a newer backend knows and this build does not - is skipped rather than
 * written, so nothing can put a key into a form that no input reads.
 *
 * <p>The answer keeps the caller's own shape, which is why this is generic over
 * the form rather than over the accepted names: a screen gets back the object it
 * passed in, fields it never offered for scanning included and untouched.
 */
export function proposalsFor<T extends Readonly<Record<string, string>>>(
  current: T,
  scan: OcrScan,
  accepted: readonly (keyof T & string)[],
): ProposalResult<T> {
  const wanted = new Set<string>(accepted)
  const patch: Record<string, string> = {}
  const statuses: Partial<Record<keyof T & string, ScanStatus>> = {}

  for (const proposal of scan.fields) {
    if (!wanted.has(proposal.field)) {
      continue
    }

    // Safe by the check above, which a Set membership test cannot narrow.
    const field = proposal.field as keyof T & string

    statuses[field] = proposal.status

    if (proposal.value !== null) {
      patch[proposal.field] = proposal.value
    }
  }

  return { values: { ...current, ...patch }, statuses }
}

/** The certificate screen's case: every field on the document is accepted. */
export function applyScan(form: CertificateForm, scan: OcrScan): ScanResult {
  const { values, statuses } = proposalsFor(form, scan, certificateFieldNames)
  return { form: values, statuses }
}

/**
 * What the screen tells the person after a scan, counted once. Deliberately
 * takes any status map: the numbers mean "of the fields this screen asked
 * about", which is thirty-two on one screen and four on the other.
 */
export function tally(statuses: Partial<Record<string, ScanStatus>>): ScanTally {
  const values = Object.values(statuses)

  return {
    detected: values.filter(status => status === 'DETECTED').length,
    needsReview: values.filter(status => status === 'NEEDS_REVIEW').length,
    notDetected: values.filter(status => status === 'NOT_DETECTED').length,
  }
}