import { apiFetch } from '../client.ts'
import { vehiclePath } from './vehicles.ts'

/**
 * The whole certificate, in the shape the backend sends and accepts. Section 8.
 *
 * <p>Nulls are real here: the backend has no NON_NULL configuration, so an empty
 * field arrives as an explicit null rather than as a missing key.
 *
 * <p><strong>Sending this replaces the optional block.</strong> A field left out
 * is cleared, not left alone - the backend narrows PATCH that way deliberately,
 * and this client always sends every field, which is what makes that safe.
 */
export interface CertificateData {
  registrationNumber: string
  firstRegistrationDate: string | null
  vehicleCategory: string | null
  make: string
  typeVariantVersion: string | null
  commercialDescription: string
  vin: string
  typeApprovalNumber: string | null
  validityPeriod: string | null
  registrationDate: string | null
  certificateIssueDate: string | null
  maximumPermissibleMassKg: number | null
  vehicleMassKg: number | null
  engineCapacityCc: number | null
  maximumPowerKw: number | null
  fuelType: string | null
  powerWeightRatio: number | null
  colour: string | null
  seats: number | null
  standingPlaces: number | null
  civNumber: string | null
  issuingAuthority: string | null
  observations: string | null
  certificateNumber: string | null
  ownerNameOrCompany: string | null
  ownerFirstName: string | null
  ownerAddress: string | null
  c2EqualsC1: boolean | null
  userNameOrCompany: string | null
  userFirstName: string | null
  userAddress: string | null
  c3EqualsC1: boolean | null
}

export function certificatePath(vehicleId: string): string {
  return `${vehiclePath(vehicleId)}/registration-certificate`
}

export function saveCertificate(vehicleId: string, data: CertificateData): Promise<CertificateData> {
  return apiFetch<CertificateData>(certificatePath(vehicleId), {
    method: 'PATCH',
    body: JSON.stringify(data),
  })
}