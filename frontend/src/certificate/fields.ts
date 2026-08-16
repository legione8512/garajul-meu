import type { CertificateData } from '../api/endpoints/certificate.ts'

export type CertificateField = keyof CertificateData

/**
 * How a value is typed on the certificate, and therefore how it is entered,
 * converted and validated. Not a rendering detail: 'integer' and 'decimal' are
 * different because the backend's columns are, and 'multiline' exists because
 * Observations is the one free-text box on the document.
 */
export type FieldKind = 'text' | 'multiline' | 'date' | 'integer' | 'decimal' | 'boolean'

/** The visual blocks of the template, which are also the reading order. */
export type FieldGroup = 'identity' | 'dates' | 'technical' | 'administrative' | 'owner' | 'legalUser'

export interface FieldSpec {
  readonly name: CertificateField
  /**
   * The official code printed on the certificate. Section 8 calls these mapping
   * metadata rather than names, which is why the code and the semantic name are
   * two separate things here. Absent for the two fields the document prints
   * without one.
   */
  readonly code?: string
  readonly kind: FieldKind
  readonly group: FieldGroup
  readonly required?: true
  /** The backend's column width, so nothing is truncated silently on the way in. */
  readonly maxLength?: number
}

/**
 * Every field, once. Rendering, conversion and validation all read this table
 * instead of repeating themselves thirty-two times - and Phase 8.3 attaches the
 * template's normalised coordinates to these same entries, so the overlay places
 * fields it already knows rather than a second list that can drift.
 *
 * <p>Order follows the document, not the database.
 */
export const certificateFields: readonly FieldSpec[] = [
  { name: 'registrationNumber', code: 'A', kind: 'text', group: 'identity', required: true, maxLength: 32 },
  { name: 'vehicleCategory', code: 'J', kind: 'text', group: 'identity', maxLength: 16 },
  { name: 'make', code: 'D.1', kind: 'text', group: 'identity', required: true, maxLength: 64 },
  { name: 'typeVariantVersion', code: 'D.2', kind: 'text', group: 'identity', maxLength: 128 },
  { name: 'commercialDescription', code: 'D.3', kind: 'text', group: 'identity', required: true, maxLength: 128 },
  { name: 'vin', code: 'E', kind: 'text', group: 'identity', required: true, maxLength: 32 },
  { name: 'typeApprovalNumber', code: 'K', kind: 'text', group: 'identity', maxLength: 64 },

  { name: 'firstRegistrationDate', code: 'B', kind: 'date', group: 'dates' },
  { name: 'validityPeriod', code: 'H', kind: 'text', group: 'dates', maxLength: 64 },
  { name: 'registrationDate', code: 'I', kind: 'date', group: 'dates' },
  { name: 'certificateIssueDate', code: 'I.1', kind: 'date', group: 'dates' },

  { name: 'maximumPermissibleMassKg', code: 'F.1', kind: 'integer', group: 'technical' },
  { name: 'vehicleMassKg', code: 'G', kind: 'integer', group: 'technical' },
  { name: 'engineCapacityCc', code: 'P.1', kind: 'integer', group: 'technical' },
  { name: 'maximumPowerKw', code: 'P.2', kind: 'decimal', group: 'technical' },
  { name: 'fuelType', code: 'P.3', kind: 'text', group: 'technical', maxLength: 32 },
  { name: 'powerWeightRatio', code: 'Q', kind: 'decimal', group: 'technical' },
  { name: 'colour', code: 'R', kind: 'text', group: 'technical', maxLength: 64 },
  { name: 'seats', code: 'S.1', kind: 'integer', group: 'technical' },
  { name: 'standingPlaces', code: 'S.2', kind: 'integer', group: 'technical' },

  { name: 'civNumber', code: 'Y', kind: 'text', group: 'administrative', maxLength: 64 },
  { name: 'issuingAuthority', code: 'Z', kind: 'text', group: 'administrative', maxLength: 128 },
  { name: 'observations', kind: 'multiline', group: 'administrative' },
  { name: 'certificateNumber', kind: 'text', group: 'administrative', maxLength: 64 },

  { name: 'ownerNameOrCompany', code: 'C.2.1', kind: 'text', group: 'owner', maxLength: 160 },
  { name: 'ownerFirstName', code: 'C.2.2', kind: 'text', group: 'owner', maxLength: 80 },
  { name: 'ownerAddress', code: 'C.2.3', kind: 'text', group: 'owner', maxLength: 255 },
  { name: 'c2EqualsC1', kind: 'boolean', group: 'owner' },

  { name: 'userNameOrCompany', code: 'C.3.1', kind: 'text', group: 'legalUser', maxLength: 160 },
  { name: 'userFirstName', code: 'C.3.2', kind: 'text', group: 'legalUser', maxLength: 80 },
  { name: 'userAddress', code: 'C.3.3', kind: 'text', group: 'legalUser', maxLength: 255 },
  { name: 'c3EqualsC1', kind: 'boolean', group: 'legalUser' },
]

/**
 * The order the groups appear in. Owner and legal user come last on purpose:
 * section 8 calls them sensitive and optional, and nothing above them depends on
 * them being filled in.
 */
export const fieldGroups: readonly FieldGroup[] = [
  'identity', 'dates', 'technical', 'administrative', 'owner', 'legalUser',
]

export function fieldsOf(group: FieldGroup): readonly FieldSpec[] {
  return certificateFields.filter(field => field.group === group)
}