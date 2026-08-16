import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import type { CertificateData } from '../api/endpoints/certificate.ts'
import type { CertificateField } from './fields.ts'
import type { FieldMessages } from '../forms/validate.ts'
import { ro } from '../i18n/locales/ro.ts'
import { CertificateOverlay } from './CertificateOverlay.tsx'
import type { FieldStatuses } from './scan.ts'
import { toForm } from './values.ts'

const EMPTY = Object.fromEntries(
  Object.keys(ro.certificate.fields).map(name => [name, null]),
) as unknown as CertificateData

function show(
  statuses: FieldStatuses = {},
  messages: FieldMessages<CertificateField> = {},
) {
  render(
    <CertificateOverlay
      form={toForm({ ...EMPTY, registrationNumber: 'B 100 ABC', make: 'Dacia', commercialDescription: 'Logan', vin: 'VF1' })}
      messages={messages}
      statuses={statuses}
      onChange={() => undefined}
    />,
  )
}

const level = (percent: number) => ro.certificate.zoomLevel.replace('{{percent}}', String(percent))

describe('CertificateOverlay', () => {
  /**
   * Section 7 asks for zoom because the template cannot usefully shrink: a field
   * is about 1.7% of the displayed width, so a phone-width certificate leaves
   * each one two pixels tall.
   */
  it('zooming changes the level in both directions', async () => {
    show()
    expect(screen.getByRole('status')).toHaveTextContent(level(100))

    await userEvent.click(screen.getByRole('button', { name: ro.certificate.zoomIn }))
    expect(screen.getByRole('status')).toHaveTextContent(level(125))

    await userEvent.click(screen.getByRole('button', { name: ro.certificate.zoomOut }))
    await userEvent.click(screen.getByRole('button', { name: ro.certificate.zoomOut }))
    expect(screen.getByRole('status')).toHaveTextContent(level(75))
  })

  it('the way back to the original size is one button', async () => {
    show()

    await userEvent.click(screen.getByRole('button', { name: ro.certificate.zoomIn }))
    await userEvent.click(screen.getByRole('button', { name: ro.certificate.zoomIn }))
    await userEvent.click(screen.getByRole('button', { name: ro.certificate.zoomReset }))

    expect(screen.getByRole('status')).toHaveTextContent(level(100))
    expect(screen.getByRole('button', { name: ro.certificate.zoomReset })).toBeDisabled()
  })

  /** A control that does nothing when pressed says so before it is pressed. */
  it('the controls stop at their limits', async () => {
    show()

    for (let click = 0; click < 10; click++) {
      const zoomIn = screen.getByRole('button', { name: ro.certificate.zoomIn })
      if ((zoomIn as HTMLButtonElement).disabled) break
      await userEvent.click(zoomIn)
    }

    expect(screen.getByRole('status')).toHaveTextContent(level(300))
    expect(screen.getByRole('button', { name: ro.certificate.zoomIn })).toBeDisabled()
  })

  /**
   * The state is drawn as a line style and also said in words. Asserting the
   * words is what makes this a test rather than a screenshot: a border is
   * invisible to anyone not looking at the picture, and section 7's three states
   * are information, not decoration.
   */
  it('a field filled by the scan says where its value came from', () => {
    show({ make: 'DETECTED' })

    expect(screen.getByLabelText(ro.certificate.fields.make))
      .toHaveAccessibleDescription(ro.certificate.scan.status.DETECTED)
  })

  it('a field the scan was unsure about asks to be checked', () => {
    show({ vin: 'NEEDS_REVIEW' })

    expect(screen.getByLabelText(ro.certificate.fields.vin))
      .toHaveAccessibleDescription(ro.certificate.scan.status.NEEDS_REVIEW)
  })

  /**
   * A box can only say one thing, and the thing worth saying is the one the
   * person has to act on. Without this the scan state would keep announcing
   * itself over a field that will not save.
   */
  it('a validation problem outranks the scan state on the same field', () => {
    show({ make: 'DETECTED' }, { make: { key: 'validation.required' } })

    const field = screen.getByLabelText(ro.certificate.fields.make)
    expect(field).toHaveAccessibleDescription(ro.validation.required)
    expect(field).toHaveAttribute('aria-invalid', 'true')
  })
})