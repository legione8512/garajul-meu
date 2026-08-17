import type { DocumentDetails } from '../api/endpoints/documents.ts'
import { maxLength, required } from '../forms/rules.ts'

/** The five fields a period carries, whether it is being added, corrected or renewed. */
export type PeriodField = 'validFrom' | 'validUntil' | 'provider' | 'referenceNumber' | 'notes'

export type PeriodValues = Record<PeriodField, string>

export const EMPTY_PERIOD: PeriodValues = {
  validFrom: '',
  validUntil: '',
  provider: '',
  referenceNumber: '',
  notes: '',
}

/**
 * Only the end date is required. Section 10.4 allows an unknown start for an ITP
 * or a rovinietă, and the lengths are the column widths so nothing is truncated
 * silently on the way in.
 */
export const periodRules = {
  validUntil: [required],
  provider: [maxLength(160)],
  referenceNumber: [maxLength(64)],
}

/** A stored document as form state, which is strings all the way down. */
export function periodOf(document: DocumentDetails): PeriodValues {
  return {
    validFrom: document.validFrom ?? '',
    validUntil: document.validUntil,
    provider: document.provider ?? '',
    referenceNumber: document.referenceNumber ?? '',
    notes: document.notes ?? '',
  }
}

/**
 * Form state as the API expects it.
 *
 * <p>An empty input means "not given", and the API expects that as null. Sending
 * `""` would store an empty string in a column whose absence is the point - and
 * on a correction, which replaces the whole record, it would be the difference
 * between clearing a note and storing a blank one.
 */
export function periodBody(values: PeriodValues) {
  return {
    validFrom: orNull(values.validFrom),
    validUntil: values.validUntil,
    provider: orNull(values.provider),
    referenceNumber: orNull(values.referenceNumber),
    notes: orNull(values.notes),
  }
}

function orNull(value: string): string | null {
  const trimmed = value.trim()
  return trimmed === '' ? null : trimmed
}