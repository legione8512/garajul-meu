import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { setAccessToken } from '../api/tokenStore.ts'
import { ro } from '../i18n/locales/ro.ts'
import { VehicleImage } from './VehicleImage.tsx'

/**
 * The component directly rather than through the route table, because what is
 * being tested is entirely local to it - the parent screen contributes nothing
 * but two props.
 *
 * <p>The cost of that shortcut is that no sign-in happens, so the token has to
 * be put in the store by hand. Without it every request goes out unauthenticated
 * and the one assertion worth making here - that the bytes are fetched *with*
 * the token - has nothing to see.
 *
 * <p><strong>jsdom implements neither createObjectURL nor revokeObjectURL.</strong>
 * They are stubbed rather than worked around, which also makes the revoke
 * assertable: leaking an object URL per replacement is invisible in a browser
 * until the tab is holding tens of megabytes, and this is the only place it can
 * be caught cheaply.
 */
const OBJECT_URL = 'blob:vehicle-photograph'
const TOKEN = 'a-test-access-token'

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function imageResponse(): Response {
  return new Response(new Blob(['bytes'], { type: 'image/jpeg' }), {
    status: 200,
    headers: { 'Content-Type': 'image/jpeg' },
  })
}

interface Calls {
  puts: number
  deletes: number
  gets: number
  lastPath: string | null
  lastAuthorization: string | null
  lastContentType: string | null
}

/**
 * Answers the image endpoint and records how it was called. `stored` starts as
 * whatever the vehicle claimed and follows the writes, so a GET after an upload
 * sees the upload - a stub that always answered the same thing could not tell a
 * replacement that worked from one that was discarded.
 *
 * <p>Unlike the page-level stubs elsewhere, this one does not branch on the
 * address, because the component reaches exactly one endpoint. It records it
 * instead: nothing else here would notice if vehicleImagePath were built wrong.
 */
function stubImage(initiallyPresent: boolean, uploadStatus = 204): Calls {
  const calls: Calls = {
    puts: 0, deletes: 0, gets: 0, lastPath: null, lastAuthorization: null, lastContentType: null,
  }
  let stored = initiallyPresent

  vi.stubGlobal('fetch', vi.fn((input: string, init?: RequestInit) => {
    const headers = new Headers(init?.headers)
    calls.lastPath = input

    if (init?.method === 'PUT') {
      calls.puts += 1
      calls.lastContentType = headers.get('Content-Type')
      if (uploadStatus !== 204) {
        return Promise.resolve(jsonResponse(uploadStatus, { code: 'IMAGE_INVALID_TYPE' }))
      }
      stored = true
      return Promise.resolve(new Response(null, { status: 204 }))
    }

    if (init?.method === 'DELETE') {
      calls.deletes += 1
      stored = false
      return Promise.resolve(new Response(null, { status: 204 }))
    }

    calls.gets += 1
    calls.lastAuthorization = headers.get('Authorization')

    return Promise.resolve(stored
      ? imageResponse()
      : jsonResponse(404, { code: 'RESOURCE_NOT_FOUND' }))
  }))

  return calls
}

function photograph(): File {
  return new File(['bytes'], 'car.jpg', { type: 'image/jpeg' })
}

describe('vehicle photograph', () => {
  beforeEach(() => {
    setAccessToken(TOKEN)
    URL.createObjectURL = vi.fn(() => OBJECT_URL)
    URL.revokeObjectURL = vi.fn()
  })

  afterEach(() => {
    setAccessToken(null)
  })

  /**
   * Nothing is fetched for a vehicle that has no photograph. The 404 would be
   * the correct answer and a predictable console error on every such screen.
   */
  it('a vehicle with no photograph offers to add one and asks the server nothing', () => {
    const calls = stubImage(false)

    render(<VehicleImage vehicleId="a" hasImage={false} />)

    expect(screen.getByText(ro.vehicleImage.none)).toBeInTheDocument()
    expect(screen.getByLabelText(ro.vehicleImage.choose)).toBeInTheDocument()
    expect(calls.gets).toBe(0)
  })

  /**
   * The assertion this whole hook exists for: the bytes are fetched from the
   * vehicle's own image address, with the token, and shown from an object URL.
   * An `<img src>` pointing at that endpoint would arrive unauthenticated and
   * answer 401.
   */
  it('an existing photograph is fetched with the token and shown from an object URL', async () => {
    const calls = stubImage(true)

    render(<VehicleImage vehicleId="a" hasImage />)

    const image = await screen.findByRole('img', { name: ro.vehicleImage.alt })

    expect(image).toHaveAttribute('src', OBJECT_URL)
    expect(calls.lastPath).toContain('/api/v1/vehicles/a/image')
    expect(calls.lastAuthorization).toBe(`Bearer ${TOKEN}`)
  })

  it('uploading shows the photograph and the control becomes replace', async () => {
    const calls = stubImage(false)

    render(<VehicleImage vehicleId="a" hasImage={false} />)

    await userEvent.upload(screen.getByLabelText(ro.vehicleImage.choose), photograph())

    expect(await screen.findByRole('img', { name: ro.vehicleImage.alt })).toBeInTheDocument()
    expect(screen.getByLabelText(ro.vehicleImage.replace)).toBeInTheDocument()
    expect(calls.puts).toBe(1)
    expect(calls.lastContentType).toBe('image/jpeg')
  })

  /**
   * The address never changes, so only the version counter can say the bytes
   * behind it have. Without it a replacement would leave the old photograph on
   * screen and nothing would look wrong.
   */
  it('replacing fetches again and revokes the object URL it is dropping', async () => {
    const calls = stubImage(true)

    render(<VehicleImage vehicleId="a" hasImage />)
    await screen.findByRole('img', { name: ro.vehicleImage.alt })

    expect(calls.gets).toBe(1)

    await userEvent.upload(screen.getByLabelText(ro.vehicleImage.replace), photograph())

    await vi.waitFor(() => { expect(calls.gets).toBe(2) })
    expect(URL.revokeObjectURL).toHaveBeenCalledWith(OBJECT_URL)
  })

  it('a refused photograph is explained and nothing is shown', async () => {
    stubImage(false, 400)

    render(<VehicleImage vehicleId="a" hasImage={false} />)

    await userEvent.upload(screen.getByLabelText(ro.vehicleImage.choose), photograph())

    expect(await screen.findByRole('alert')).toHaveTextContent(ro.errors.IMAGE_INVALID_TYPE)
    expect(screen.queryByRole('img', { name: ro.vehicleImage.alt })).not.toBeInTheDocument()
  })

  /** One click must not be enough, exactly as for deleting the vehicle itself. */
  it('deleting asks before it deletes', async () => {
    const calls = stubImage(true)

    render(<VehicleImage vehicleId="a" hasImage />)
    await screen.findByRole('img', { name: ro.vehicleImage.alt })

    await userEvent.click(screen.getByRole('button', { name: ro.vehicleImage.delete }))

    expect(screen.getByText(ro.vehicleImage.confirmDelete)).toBeInTheDocument()
    expect(calls.deletes).toBe(0)
  })

  it('confirming removes the photograph and the control goes back to add', async () => {
    const calls = stubImage(true)

    render(<VehicleImage vehicleId="a" hasImage />)
    await screen.findByRole('img', { name: ro.vehicleImage.alt })

    await userEvent.click(screen.getByRole('button', { name: ro.vehicleImage.delete }))
    await userEvent.click(screen.getByRole('button', { name: ro.vehicleImage.confirmDeleteYes }))

    expect(await screen.findByText(ro.vehicleImage.none)).toBeInTheDocument()
    expect(screen.getByLabelText(ro.vehicleImage.choose)).toBeInTheDocument()
    expect(screen.queryByRole('img', { name: ro.vehicleImage.alt })).not.toBeInTheDocument()
    expect(calls.deletes).toBe(1)
  })
})