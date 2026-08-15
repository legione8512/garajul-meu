import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import i18n from '../i18n/config.ts'
import { ro } from '../i18n/locales/ro.ts'
import { paths } from '../routes/paths.ts'
import { renderApp } from '../test/renderApp.tsx'

const PROFILE = {
  id: '1', fullName: 'Marius Robert', email: 'marius@example.com',
  preferredLanguage: 'ro', timezone: 'Europe/Bucharest', emailVerified: true,
}

const CREATED = {
  id: 'a', registrationNumber: 'B 100 ABC', make: 'Dacia',
  commercialDescription: 'Logan', vin: 'VF1AAAAAAAA000001', createdAt: '2026-08-15T10:00:00Z',
}

interface Sent {
  posts: number
  body: Record<string, unknown> | null
}

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

/**
 * Answers by path and method, with no catch-all standing in for a request
 * nobody thought about - the pattern that let a change to the garage break a
 * test in the sign-in file.
 *
 * <p>Reads answer with a canonical vehicle rather than echoing what was just
 * posted. This screen is responsible for the request it sends and for where it
 * goes next; what the vehicle's own screen renders is that screen's test. So
 * every successful submission here lands on a heading reading "Dacia Logan",
 * whatever nickname was typed, and the nickname is asserted on the request body
 * where this screen actually decides it.
 */
function stubVehicles(onCreate: () => Response): Sent {
  const sent: Sent = { posts: 0, body: null }

  vi.stubGlobal('fetch', vi.fn((input: string, init?: RequestInit) => {
    if (input.includes('/auth/refresh')) {
      return Promise.resolve(jsonResponse(200, {
        accessToken: 'fresh', expiresInSeconds: 600, refreshToken: null,
      }))
    }
    if (input.includes('/api/v1/vehicles')) {
      if (init?.method === 'POST') {
        sent.posts += 1
        sent.body = JSON.parse(init.body as string) as Record<string, unknown>
        return Promise.resolve(onCreate())
      }
      // The garage list, or the new vehicle's own screen.
      return Promise.resolve(jsonResponse(200, input.endsWith('/vehicles') ? [] : CREATED))
    }
    return Promise.resolve(jsonResponse(200, PROFILE))
  }))

  return sent
}

async function fillInRequired() {
  await userEvent.type(screen.getByLabelText(ro.fields.registrationNumber), 'B 100 ABC')
  await userEvent.type(screen.getByLabelText(ro.fields.make), 'Dacia')
  await userEvent.type(screen.getByLabelText(ro.fields.commercialDescription), 'Logan')
  await userEvent.type(screen.getByLabelText(ro.fields.vin), 'VF1AAAAAAAA000001')
}

describe('add vehicle', () => {
  it('refuses an incomplete form without touching the network', async () => {
    const sent = stubVehicles(() => jsonResponse(201, CREATED))

    renderApp(paths.addVehicle)
    await screen.findByRole('heading', { level: 1, name: ro.screens.addVehicle })

    await userEvent.click(screen.getByRole('button', { name: ro.addVehicle.submit }))

    // Asserted per field: all four are empty and produce the same message, so
    // what matters is that each one is tied to its own input.
    expect(screen.getByLabelText(ro.fields.vin)).toHaveAccessibleDescription(ro.validation.required)
    expect(screen.getByLabelText(ro.fields.make)).toHaveAccessibleDescription(ro.validation.required)
    expect(sent.posts).toBe(0)
  })

  it('creates the vehicle and opens it', async () => {
    const sent = stubVehicles(() => jsonResponse(201, CREATED))

    renderApp(paths.addVehicle)
    await screen.findByRole('heading', { level: 1, name: ro.screens.addVehicle })

    await fillInRequired()
    await userEvent.type(screen.getByLabelText(ro.fields.displayName), 'Mașina de teren')
    await userEvent.click(screen.getByRole('button', { name: ro.addVehicle.submit }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Dacia Logan' })).toBeInTheDocument()
    expect(sent.posts).toBe(1)
    expect(sent.body).toMatchObject({
      registrationNumber: 'B 100 ABC',
      vin: 'VF1AAAAAAAA000001',
      displayName: 'Mașina de teren',
    })
  })

  /**
   * The nickname is optional, and a blank one is sent as typed. The backend
   * turns it into nothing stored, which keeps the meaning of "no nickname" in
   * one place instead of two that can disagree.
   */
  it('the nickname is optional and a blank one goes as typed', async () => {
    const sent = stubVehicles(() => jsonResponse(201, CREATED))

    renderApp(paths.addVehicle)
    await screen.findByRole('heading', { level: 1, name: ro.screens.addVehicle })

    await fillInRequired()
    await userEvent.click(screen.getByRole('button', { name: ro.addVehicle.submit }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Dacia Logan' })).toBeInTheDocument()
    expect(sent.body?.displayName).toBe('')
  })

  /** A duplicate VIN is about the vehicle, not about one input, so it belongs in the banner. */
  it('reports a duplicate VIN as a form failure rather than a field error', async () => {
    stubVehicles(() => jsonResponse(409, { code: 'VEHICLE_DUPLICATE_VIN', fieldErrors: [] }))

    renderApp(paths.addVehicle)
    await screen.findByRole('heading', { level: 1, name: ro.screens.addVehicle })

    await fillInRequired()
    await userEvent.click(screen.getByRole('button', { name: ro.addVehicle.submit }))

    expect(await screen.findByRole('alert')).toHaveTextContent(ro.errors.VEHICLE_DUPLICATE_VIN)
    expect(screen.getByLabelText(ro.fields.vin)).not.toHaveAttribute('aria-invalid', 'true')
  })

  /**
   * The case the client-side rules exist for and cannot cover: the backend
   * disagrees about a bound this form thought was fine. The form answers in its
   * own wording, because the server sends a constraint name and no numbers.
   */
  it('puts a field error from the server on the field it belongs to', async () => {
    stubVehicles(() => jsonResponse(400, {
      code: 'VALIDATION_ERROR',
      fieldErrors: [{ field: 'vin', constraint: 'Size' }],
    }))

    renderApp(paths.addVehicle)
    await screen.findByRole('heading', { level: 1, name: ro.screens.addVehicle })

    await fillInRequired()
    await userEvent.click(screen.getByRole('button', { name: ro.addVehicle.submit }))

    const vin = await screen.findByLabelText(ro.fields.vin)
    expect(vin).toHaveAttribute('aria-invalid', 'true')
    expect(vin).toHaveAccessibleDescription(i18n.t('validation.maxLength', { max: 32 }))
  })
})