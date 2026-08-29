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

  /**
   * The release blocker of 2026-08-25, and the only part of it a test can hold.
   *
   * <p>The overlay draws on a photograph of a printed document, so it must state
   * its own ink rather than inherit the application's. It did not, and when 13.5
   * inverted the palette every value on this screen went to 1.01:1 against the
   * template - rendered, and invisible - for two days in production.
   *
   * <p><strong>This cannot measure contrast and does not pretend to.</strong>
   * jsdom has no layout and never loads the template, so the real check is the
   * browser walkthrough that found it. What this holds is the one thing a unit
   * test can: that a concrete, dark colour is declared here at all. Deleting the
   * line, or "tidying" it to `var(--text)` so it matches the palette, fails
   * here - and that tidy is exactly the edit that caused the defect.
   */
  it('a field states its own ink rather than inheriting the page palette', () => {
    show()

    const declared = screen.getByLabelText(ro.certificate.fields.make).style.color

    expect(declared, 'the field declares no colour and will inherit --text').not.toBe('')

    const channels = declared.match(/\d+/g)
    expect(channels, `the colour is "${declared}", which is not a concrete value`).not.toBeNull()

    // Crude brightness rather than a contrast ratio, and deliberately so: the
    // template is pale everywhere it holds a field, so "is the ink dark" is the
    // whole question a test without pixels can ask. --text lands near 0.91 and
    // the ink near 0.08, so the threshold separates them with room to spare.
    const [red, green, blue] = (channels ?? []).map(Number)
    const brightness = (0.2126 * red + 0.7152 * green + 0.0722 * blue) / 255

    expect(brightness, `the ink is light at ${brightness.toFixed(2)}, and the template is pale`)
      .toBeLessThan(0.3)
  })
})