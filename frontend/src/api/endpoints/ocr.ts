import { CERTIFICATE_SCAN_CEILING, withinCeiling } from '../../images/shrink.ts'
import { apiFetch } from '../client.ts'

/** The three states section 7 puts on the overlay. */
export type ScanStatus = 'DETECTED' | 'NEEDS_REVIEW' | 'NOT_DETECTED'

/**
 * One proposal from a scan.
 *
 * <p><strong>`field` is a string and not a CertificateField on purpose.</strong>
 * The backend is a separate deployment and may be a version ahead of this build;
 * naming the type here would be an assertion the compiler cannot check and would
 * turn a field this build has never heard of into a silently invented key. It is
 * narrowed against the field table instead, where the check is real.
 *
 * <p>`value` is already in the form the input can hold - an ISO date, digits for
 * a number - or null when nothing usable was read. That is the backend's
 * promise, and it is why applying a scan needs no parsing on this side.
 */
export interface ProposedField {
  readonly field: string
  readonly value: string | null
  readonly confidence: number
  readonly status: ScanStatus
}

/** Every coded field, exactly once, including the ones nothing was found for. */
export interface OcrScan {
  readonly fields: readonly ProposedField[]
}

export const ocrCertificatePath = '/api/v1/ocr/registration-certificate'

/**
 * Sends one photograph and gets proposals back. Nothing is saved by this call:
 * section 13 requires the person to review and correct first, so the certificate
 * screen's own PATCH remains the only thing that stores anything.
 *
 * <p>The Content-Type is deliberately not set. See `send` in the client.
 *
 * <p><strong>The photograph is shrunk only if it exceeds what the server will
 * take.</strong> Every pixel matters here more than anywhere else in the
 * application - the `I` family is unreadable to Document AI and the recorded
 * trigger for revisiting that is a higher-resolution photograph - so a scan
 * within the limit is sent exactly as it was taken.
 */
export async function scanCertificate(image: File): Promise<OcrScan> {
  const body = new FormData()
  body.append('image', await withinCeiling(image, CERTIFICATE_SCAN_CEILING))

  return await apiFetch<OcrScan>(ocrCertificatePath, { method: 'POST', body })
}