import { render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { setAccessToken } from '../api/tokenStore.ts'
import { VehicleThumbnail } from './VehicleThumbnail.tsx'

/**
 * The card's round picture, rendered on its own: the two cards that carry it
 * contribute nothing but the identifier and the flag.
 *
 * <p>As in VehicleImage's tests, the token is put in the store by hand - no sign
 * -in happens here - and jsdom's missing object URL functions are stubbed, which
 * is also what makes the address the component asked for assertable.
 */
const OBJECT_URL = 'blob:vehicle-thumbnail'

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

interface Calls {
  gets: number
  lastPath: string | null
  lastAuthorization: string | null
}

function stubThumbnail(status = 200): Calls {
  const calls: Calls = { gets: 0, lastPath: null, lastAuthorization: null }

  vi.stubGlobal('fetch', vi.fn((input: string, init?: RequestInit) => {
    calls.gets += 1
    calls.lastPath = input
    calls.lastAuthorization = new Headers(init?.headers).get('Authorization')

    if (status !== 200) {
      return Promise.resolve(jsonResponse(status, { code: 'RESOURCE_NOT_FOUND' }))
    }

    return Promise.resolve(new Response(new Blob(['bytes'], { type: 'image/jpeg' }), {
      status: 200,
      headers: { 'Content-Type': 'image/jpeg' },
    }))
  }))

  return calls
}

describe('vehicle thumbnail', () => {
  beforeEach(() => {
    setAccessToken('a-test-access-token')
    URL.createObjectURL = vi.fn(() => OBJECT_URL)
    URL.revokeObjectURL = vi.fn()
  })

  afterEach(() => {
    setAccessToken(null)
  })

  /** Most vehicles have no photograph, and a request per card would be a 404 per card. */
  it('draws the empty circle for a vehicle with no photograph, and asks for nothing', () => {
    const calls = stubThumbnail()

    const { container } = render(<VehicleThumbnail vehicleId="a" hasImage={false} />)

    expect(container.querySelector('[data-thumbnail="empty"]')).not.toBeNull()
    expect(container.querySelector('svg')).not.toBeNull()
    expect(calls.gets).toBe(0)
  })

  /**
   * The small square rather than the photograph itself, which is the whole point
   * of the endpoint: a garage of five cars would otherwise pull megabytes to fill
   * five circles.
   */
  it('asks for the thumbnail with the token and shows what comes back', async () => {
    const calls = stubThumbnail()

    const { container } = render(<VehicleThumbnail vehicleId="a" hasImage />)

    await waitFor(() => {
      expect(container.querySelector('img')).toHaveAttribute('src', OBJECT_URL)
    })

    expect(container.querySelector('[data-thumbnail="photo"]')).not.toBeNull()
    // The base is the client's; what matters here is which of the two pictures
    // was asked for.
    expect(calls.lastPath).toMatch(/\/api\/v1\/vehicles\/a\/image\/thumbnail$/)
    expect(calls.lastAuthorization).toBe('Bearer a-test-access-token')
  })

  /**
   * A decoration that could not be fetched is not something the reader can act
   * on. The circle stays empty and the card says nothing about it - the screen
   * that manages the photograph is where a failure is worth reporting.
   */
  it('keeps the circle empty and raises no alarm when the picture cannot be had', async () => {
    stubThumbnail(404)

    const { container } = render(<VehicleThumbnail vehicleId="a" hasImage />)

    await waitFor(() => {
      expect(container.querySelector('[data-thumbnail="empty"]')).not.toBeNull()
    })

    expect(container.querySelector('img')).toBeNull()
    expect(screen.queryByRole('alert')).toBeNull()
  })

  /** Hidden from assistive technology: the card already names the vehicle in words. */
  it('says nothing to a screen reader', () => {
    stubThumbnail()

    const { container } = render(<VehicleThumbnail vehicleId="a" hasImage={false} />)

    expect(container.querySelector('[data-thumbnail]')).toHaveAttribute('aria-hidden', 'true')
  })
})
