import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import { ro } from '../i18n/locales/ro.ts'
import { paths } from '../routes/paths.ts'
import { renderApp } from '../test/renderApp.tsx'

const PROFILE = {
  id: '1', fullName: 'Marius Robert', email: 'marius@example.com',
  preferredLanguage: 'ro', timezone: 'Europe/Bucharest', emailVerified: true,
}

const LOGAN = {
  id: 'a', registrationNumber: 'B 100 ABC', make: 'Dacia', commercialDescription: 'Logan',
  vin: 'VF1AAAAAAAA000001', createdAt: '2026-08-15T10:00:00Z',
}

interface Stored {
  vehicle: Record<string, unknown>
  deletes: number
}

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

/**
 * Keeps the vehicle in memory so a PATCH is visible to the GET that follows it.
 * A stub that always answered the same thing could not tell a rename that worked
 * from one that was silently discarded.
 */
function stubVehicle(initial: Record<string, unknown>): Stored {
  const stored: Stored = { vehicle: { ...initial }, deletes: 0 }

  vi.stubGlobal('fetch', vi.fn((input: string, init?: RequestInit) => {
    if (input.includes('/auth/refresh')) {
      return Promise.resolve(jsonResponse(200, {
        accessToken: 'fresh', expiresInSeconds: 600, refreshToken: null,
      }))
    }
    if (input.includes('/api/v1/vehicles/')) {
      if (init?.method === 'PATCH') {
        const sent = JSON.parse(init.body as string) as { displayName: string }
        stored.vehicle = sent.displayName === ''
          ? { ...stored.vehicle, displayName: undefined }
          : { ...stored.vehicle, displayName: sent.displayName }
        return Promise.resolve(jsonResponse(200, stored.vehicle))
      }
      if (init?.method === 'DELETE') {
        stored.deletes += 1
        return Promise.resolve(new Response(null, { status: 204 }))
      }
      return Promise.resolve(jsonResponse(200, stored.vehicle))
    }
    if (input.includes('/api/v1/vehicles')) {
      // The garage, which is where a deletion lands.
      return Promise.resolve(jsonResponse(200, []))
    }
    return Promise.resolve(jsonResponse(200, PROFILE))
  }))

  return stored
}

describe('vehicle details', () => {
  it('shows the identity held on the certificate', async () => {
    stubVehicle(LOGAN)

    renderApp(paths.vehicle('a'))

    expect(await screen.findByRole('heading', { level: 1, name: 'Dacia Logan' })).toBeInTheDocument()
    expect(screen.getByText('B 100 ABC')).toBeInTheDocument()
    expect(screen.getByText('VF1AAAAAAAA000001')).toBeInTheDocument()
  })

  /**
   * What somebody following a stale link sees - and what somebody guessing at
   * another account's identifiers sees, since the backend answers the same 404
   * either way rather than admitting the vehicle exists.
   */
  it('says so when the vehicle is not there', async () => {
    vi.stubGlobal('fetch', vi.fn((input: string) => {
      if (input.includes('/auth/refresh')) {
        return Promise.resolve(jsonResponse(200, {
          accessToken: 'fresh', expiresInSeconds: 600, refreshToken: null,
        }))
      }
      if (input.includes('/api/v1/vehicles/')) {
        return Promise.resolve(jsonResponse(404, { code: 'VEHICLE_NOT_FOUND' }))
      }
      return Promise.resolve(jsonResponse(200, PROFILE))
    }))

    renderApp(paths.vehicle('does-not-matter'))

    expect(await screen.findByRole('alert')).toHaveTextContent(ro.errors.VEHICLE_NOT_FOUND)
    expect(screen.queryByRole('button', { name: ro.vehicle.delete })).not.toBeInTheDocument()
  })

  it('renaming shows the new name', async () => {
    stubVehicle(LOGAN)

    renderApp(paths.vehicle('a'))
    await screen.findByRole('heading', { level: 1, name: 'Dacia Logan' })

    await userEvent.type(screen.getByLabelText(ro.fields.displayName), 'Mașina de teren')
    await userEvent.click(screen.getByRole('button', { name: ro.vehicle.rename }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Mașina de teren' })).toBeInTheDocument()
  })

  /** Clearing it is the only way to remove one, and the label falls back. */
  it('clearing the nickname returns the vehicle to its certificate name', async () => {
    stubVehicle({ ...LOGAN, displayName: 'Vechea poreclă' })

    renderApp(paths.vehicle('a'))
    await screen.findByRole('heading', { level: 1, name: 'Vechea poreclă' })

    await userEvent.clear(screen.getByLabelText(ro.fields.displayName))
    await userEvent.click(screen.getByRole('button', { name: ro.vehicle.rename }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Dacia Logan' })).toBeInTheDocument()
  })

  /** One click must not be enough. This is the assertion that keeps it that way. */
  it('deleting asks before it deletes', async () => {
    const stored = stubVehicle(LOGAN)

    renderApp(paths.vehicle('a'))
    await screen.findByRole('heading', { level: 1, name: 'Dacia Logan' })

    await userEvent.click(screen.getByRole('button', { name: ro.vehicle.delete }))

    expect(screen.getByText(ro.vehicle.confirmDelete)).toBeInTheDocument()
    expect(stored.deletes).toBe(0)
  })

  it('confirming deletes the vehicle and returns to the garage', async () => {
    const stored = stubVehicle(LOGAN)

    renderApp(paths.vehicle('a'))
    await screen.findByRole('heading', { level: 1, name: 'Dacia Logan' })

    await userEvent.click(screen.getByRole('button', { name: ro.vehicle.delete }))
    await userEvent.click(screen.getByRole('button', { name: ro.vehicle.confirmDeleteYes }))

    expect(await screen.findByRole('heading', { level: 1, name: ro.screens.garage })).toBeInTheDocument()
    expect(stored.deletes).toBe(1)
  })

  it('changing your mind deletes nothing', async () => {
    const stored = stubVehicle(LOGAN)

    renderApp(paths.vehicle('a'))
    await screen.findByRole('heading', { level: 1, name: 'Dacia Logan' })

    await userEvent.click(screen.getByRole('button', { name: ro.vehicle.delete }))
    await userEvent.click(screen.getByRole('button', { name: ro.vehicle.cancel }))

    expect(screen.queryByText(ro.vehicle.confirmDelete)).not.toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 1, name: 'Dacia Logan' })).toBeInTheDocument()
    expect(stored.deletes).toBe(0)
  })
})