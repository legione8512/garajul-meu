import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import type { CertificateData } from '../api/endpoints/certificate.ts'
import { ro } from '../i18n/locales/ro.ts'
import { CertificateOverlay } from './CertificateOverlay.tsx'
import { toForm } from './values.ts'

const EMPTY = Object.fromEntries(
  Object.keys(ro.certificate.fields).map(name => [name, null]),
) as unknown as CertificateData

function show() {
  render(
    <CertificateOverlay
      form={toForm({ ...EMPTY, registrationNumber: 'B 100 ABC', make: 'Dacia', commercialDescription: 'Logan', vin: 'VF1' })}
      messages={{}}
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
})