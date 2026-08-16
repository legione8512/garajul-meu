import type { CertificateData } from '../api/endpoints/certificate.ts'
import { certificateFields, type CertificateField } from './fields.ts'

/** Form state is strings throughout, because that is what an input holds. */
export type CertificateForm = Record<CertificateField, string>

/**
 * The certificate as the form holds it: every value a string, every absent value
 * the empty string.
 *
 * <p>A checkbox is 'true' when ticked and '' when not, so one map covers all
 * four kinds rather than a second piece of state existing for two booleans.
 */
export function toForm(data: CertificateData): CertificateForm {
  const form = {} as CertificateForm

  for (const field of certificateFields) {
    const value = data[field.name]

    form[field.name] = value === null || value === undefined
      ? ''
      : field.kind === 'boolean'
        ? (value === true ? 'true' : '')
        : String(value)
  }

  return form
}

/**
 * Reads a number the way the person in front of the screen writes one.
 *
 * <p>This application is Romanian, and Romanian writes 66,5 where English writes
 * 66.5. Number() answers NaN for the first, which this function would turn into
 * null - so somebody entering the power of their own car in their own language
 * would watch the field empty itself on save, with nothing said. That is silent
 * data loss, and it is the reason this is not a one-line Number() call.
 *
 * <p>Whole numbers keep only their digits, whichever way the person groups them:
 * a whole number has no decimal separator, so every other character is grouping
 * and 1.780, 1 780 and 1780 are the same value. Decimals treat a comma as the
 * decimal point, because that is the convention here.
 */
function parseNumber(raw: string, kind: 'integer' | 'decimal'): number | null {
  const cleaned = kind === 'integer'
    ? raw.replace(/[^\d-]/g, '')
    : raw.replace(/\s/g, '').replace(',', '.')

  if (cleaned === '' || cleaned === '-') {
    return null
  }

  const parsed = Number(cleaned)
  return Number.isFinite(parsed) ? parsed : null
}

/**
 * The form as the backend wants it: empty strings back to nulls, numbers as
 * numbers rather than as digits in quotes.
 *
 * <p>A value that is not a number at all becomes null rather than NaN, which
 * would serialise to JSON null anyway but only after passing through a value no
 * arithmetic could survive. The field's own validation reports it; this only has
 * to avoid inventing data.
 *
 * <p><strong>An untouched checkbox is sent as false, not null.</strong> The
 * certificate's box is either ticked or it is not, and the two meanings the
 * backend can store - unknown and unticked - are the same thing to anyone
 * looking at the document.
 */
export function fromForm(form: CertificateForm): CertificateData {
  const data = {} as Record<CertificateField, unknown>

  for (const field of certificateFields) {
    const raw = form[field.name].trim()

    if (field.kind === 'boolean') {
      data[field.name] = raw === 'true'
      continue
    }

    if (raw === '') {
      data[field.name] = null
      continue
    }

    if (field.kind === 'integer' || field.kind === 'decimal') {
      data[field.name] = parseNumber(raw, field.kind)
      continue
    }

    data[field.name] = raw
  }

  return data as unknown as CertificateData
}