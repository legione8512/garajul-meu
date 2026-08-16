import type { CertificateField } from './fields.ts'

export interface FieldPosition {
  readonly name: CertificateField
  /** Fractions of the template's width and height, so the image can be replaced. */
  readonly x: number
  readonly y: number
  readonly w: number
  readonly h: number
}

/**
 * Where each field sits on the approved template. Specification section 7.
 *
 * <p><strong>Measured, not guessed</strong>, which section 7 demands in those
 * words. The template was decoded pixel by pixel: the printed code labels - A,
 * J, D.1 - were located by their dark text, which gives each row's centre line,
 * and the panel edges and the column divider were found from the border colour.
 * The result was then drawn back over the image and checked by eye before being
 * written here.
 *
 * <p>Normalised on purpose. The approved image is 1280x666, which is modest for
 * the zoom section 7 asks for; a sharper scan dropped in its place needs no
 * recalibration because nothing here is in pixels.
 *
 * <p><strong>A field may appear more than once.</strong> The certificate prints
 * "Numărul certificatului" on two panels, and section 10.3 stores it once - so
 * two positions share one value, and editing either changes both. This is why
 * the table is a list rather than a map keyed by field.
 *
 * <p>There is no entry for the ITP annex on the right-hand panel. Section 8
 * makes X an ingestion source that becomes a VehicleDocument; it is drawn as
 * part of the image and carries no editable certificate field.
 */
export const fieldPositions: readonly FieldPosition[] = [
  // Left panel: identity, one row per line.
  { name: 'registrationNumber', x: 0.0453, y: 0.0495, w: 0.2719, h: 0.0330 },
  { name: 'vehicleCategory', x: 0.0453, y: 0.0871, w: 0.2719, h: 0.0330 },
  { name: 'make', x: 0.0453, y: 0.1231, w: 0.2719, h: 0.0330 },
  { name: 'typeVariantVersion', x: 0.0453, y: 0.1592, w: 0.2719, h: 0.0330 },
  { name: 'commercialDescription', x: 0.0453, y: 0.1967, w: 0.2719, h: 0.0330 },
  { name: 'vin', x: 0.0453, y: 0.2387, w: 0.2719, h: 0.0330 },
  { name: 'typeApprovalNumber', x: 0.0453, y: 0.2748, w: 0.2719, h: 0.0330 },

  // Left panel: C.2, the owner.
  { name: 'ownerNameOrCompany', x: 0.0938, y: 0.3123, w: 0.2234, h: 0.0330 },
  { name: 'ownerFirstName', x: 0.0219, y: 0.3859, w: 0.2953, h: 0.0330 },
  { name: 'ownerAddress', x: 0.0219, y: 0.4595, w: 0.2953, h: 0.0330 },
  { name: 'c2EqualsC1', x: 0.2578, y: 0.5751, w: 0.0125, h: 0.0240 },

  // Left panel: C.3, the legal user.
  { name: 'userNameOrCompany', x: 0.0938, y: 0.6126, w: 0.2234, h: 0.0330 },
  { name: 'userFirstName', x: 0.0219, y: 0.6832, w: 0.2953, h: 0.0330 },
  { name: 'userAddress', x: 0.0219, y: 0.7598, w: 0.2953, h: 0.0330 },
  { name: 'c3EqualsC1', x: 0.2578, y: 0.8829, w: 0.0125, h: 0.0240 },

  // Middle panel, left column.
  { name: 'firstRegistrationDate', x: 0.3844, y: 0.0465, w: 0.1078, h: 0.0330 },
  { name: 'registrationDate', x: 0.3844, y: 0.0841, w: 0.1078, h: 0.0330 },
  { name: 'maximumPermissibleMassKg', x: 0.3844, y: 0.1216, w: 0.1078, h: 0.0330 },
  { name: 'engineCapacityCc', x: 0.3844, y: 0.1592, w: 0.1078, h: 0.0330 },
  { name: 'fuelType', x: 0.3844, y: 0.1967, w: 0.1078, h: 0.0330 },
  { name: 'seats', x: 0.3844, y: 0.2718, w: 0.1078, h: 0.0330 },
  { name: 'civNumber', x: 0.3844, y: 0.3093, w: 0.0844, h: 0.0330 },

  // Middle panel, right column.
  { name: 'validityPeriod', x: 0.5313, y: 0.0465, w: 0.1172, h: 0.0330 },
  { name: 'certificateIssueDate', x: 0.5313, y: 0.0841, w: 0.1172, h: 0.0330 },
  { name: 'vehicleMassKg', x: 0.5313, y: 0.1216, w: 0.1172, h: 0.0330 },
  { name: 'maximumPowerKw', x: 0.5313, y: 0.1592, w: 0.1172, h: 0.0330 },
  { name: 'powerWeightRatio', x: 0.5313, y: 0.1967, w: 0.1172, h: 0.0330 },
  { name: 'standingPlaces', x: 0.5313, y: 0.2718, w: 0.1172, h: 0.0330 },
  { name: 'issuingAuthority', x: 0.4828, y: 0.3093, w: 0.1656, h: 0.0330 },

  // R spans both columns; Observations is the one large box.
  { name: 'colour', x: 0.3844, y: 0.2342, w: 0.2641, h: 0.0330 },
  { name: 'observations', x: 0.3531, y: 0.3784, w: 0.2953, h: 0.5180 },

  // Printed twice, stored once.
  { name: 'certificateNumber', x: 0.1187, y: 0.9159, w: 0.1984, h: 0.0300 },
  { name: 'certificateNumber', x: 0.4375, y: 0.9159, w: 0.2109, h: 0.0300 },
]