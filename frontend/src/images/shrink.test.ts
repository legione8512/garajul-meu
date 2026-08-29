import { afterEach, describe, expect, it, vi } from 'vitest'

import { CERTIFICATE_SCAN_CEILING, VEHICLE_IMAGE_CEILING, withinCeiling } from './shrink.ts'

/**
 * A file of a stated size without allocating it. `size` is the only property the
 * decision to shrink turns on, and nothing reads a byte of the input.
 */
function fileOf(bytes: number, name = 'photo.jpg', type = 'image/jpeg'): File {
  const file = new File(['x'], name, { type })
  Object.defineProperty(file, 'size', { value: bytes })
  return file
}

/**
 * Stands in for the whole browser pipeline: the decoder, the canvas and the
 * encoder. `encodedSizes` is consumed one entry per pass, which is what lets a
 * test say "the first attempt was still too big" without producing an image.
 *
 * <p><strong>Real bytes, not a faked `size`.</strong> The first version of this
 * file defined `size` on the blob and left its content one character long -
 * which fooled the loop, since that only reads the property, and was then caught
 * by `new File([encoded], ...)`, which copies actual content and reported one
 * byte. The production code was right and the double was lying. Sizes here are
 * therefore allocated for real, and kept small enough that doing so is cheap.
 */
function stubPipeline(options: {
  width: number
  height: number
  encodedSizes: number[]
}) {
  const closed = { count: 0 }
  const drawnTo: Array<[number, number]> = []

  vi.stubGlobal('createImageBitmap', vi.fn(() => Promise.resolve({
    width: options.width,
    height: options.height,
    close: () => { closed.count++ },
  })))

  const remaining = [...options.encodedSizes]

  vi.spyOn(HTMLCanvasElement.prototype, 'getContext')
    .mockReturnValue({ drawImage: () => undefined } as unknown as CanvasRenderingContext2D)

  vi.spyOn(HTMLCanvasElement.prototype, 'toBlob')
    .mockImplementation(function (this: HTMLCanvasElement, callback) {
      drawnTo.push([this.width, this.height])
      const size = remaining.shift()
      callback(size === undefined ? null : new Blob([new Uint8Array(size)], { type: 'image/jpeg' }))
    })

  return { closed, drawnTo }
}

/** Anything above this is re-encoded; anything at or below it is accepted. */
const VEHICLE_TARGET = VEHICLE_IMAGE_CEILING.maxBytes * 0.8

afterEach(() => {
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

describe('fitting a photograph under the server ceiling', () => {
  /**
   * The rule the certificate scan depends on. Document AI cannot read the `I`
   * family today and the recorded trigger for revisiting that is a photograph at
   * *higher* resolution, so a scan the server would have accepted must arrive
   * with every pixel it was taken with. Identity, not equality: nothing was
   * re-encoded.
   */
  it('leaves a file the server would accept exactly as it is', async () => {
    const original = fileOf(CERTIFICATE_SCAN_CEILING.maxBytes - 1)

    await expect(withinCeiling(original, CERTIFICATE_SCAN_CEILING)).resolves.toBe(original)
  })

  it('leaves a file sitting exactly on the ceiling alone', async () => {
    const original = fileOf(VEHICLE_IMAGE_CEILING.maxBytes)

    await expect(withinCeiling(original, VEHICLE_IMAGE_CEILING)).resolves.toBe(original)
  })

  /**
   * <strong>The bet the whole module rests on.</strong> jsdom has no
   * `createImageBitmap`, which makes this the real environment rather than a
   * contrived one - and it is exactly what an old browser looks like. The upload
   * must still be attempted with the original file, so the person meets the
   * server's honest IMAGE_TOO_LARGE rather than a crash from the code that was
   * meant to help.
   */
  it('sends the original when the browser cannot shrink at all', async () => {
    const original = fileOf(VEHICLE_IMAGE_CEILING.maxBytes + 1_000_000)

    await expect(withinCeiling(original, VEHICLE_IMAGE_CEILING)).resolves.toBe(original)
  })

  it('sends the original when the decoder refuses the file', async () => {
    vi.stubGlobal('createImageBitmap', vi.fn(() => Promise.reject(new Error('not an image'))))
    const original = fileOf(VEHICLE_IMAGE_CEILING.maxBytes + 1)

    await expect(withinCeiling(original, VEHICLE_IMAGE_CEILING)).resolves.toBe(original)
  })

  it('sends the original when the encoder produces nothing', async () => {
    stubPipeline({ width: 4000, height: 3000, encodedSizes: [] })
    const original = fileOf(VEHICLE_IMAGE_CEILING.maxBytes + 1)

    await expect(withinCeiling(original, VEHICLE_IMAGE_CEILING)).resolves.toBe(original)
  })

  it('shrinks an over-sized photograph and hands back JPEG bytes', async () => {
    stubPipeline({ width: 4000, height: 3000, encodedSizes: [2_000_000] })
    const original = fileOf(VEHICLE_IMAGE_CEILING.maxBytes + 3_000_000, 'masina.png', 'image/png')

    const sent = await withinCeiling(original, VEHICLE_IMAGE_CEILING)

    expect(sent).not.toBe(original)
    expect(sent.size).toBe(2_000_000)
    // The bytes are JPEG whatever they were, so the name must not still say PNG.
    expect(sent.name).toBe('masina.jpg')
    expect(sent.type).toBe('image/jpeg')
  })

  /**
   * No formula predicts a JPEG's size from its pixel count, so the loop measures
   * instead of calculating. This is the pass that proves it measures: the first
   * encode comes back over target and a second, smaller one is taken.
   */
  it('tries again smaller when the first encode is still too large', async () => {
    const { drawnTo } = stubPipeline({
      width: 4000,
      height: 3000,
      encodedSizes: [Math.round(VEHICLE_TARGET) + 100_000, 1_500_000],
    })
    const original = fileOf(VEHICLE_IMAGE_CEILING.maxBytes + 5_000_000)

    const sent = await withinCeiling(original, VEHICLE_IMAGE_CEILING)

    expect(sent.size).toBe(1_500_000)
    expect(drawnTo).toHaveLength(2)
    expect(drawnTo[1][0]).toBeLessThan(drawnTo[0][0])
  })

  /**
   * Shrinking past the server's own `min-side` would trade one rejection for
   * another, and the second would be about the wrong thing. One pass at this
   * shape already takes the short side under two hundred.
   */
  it('gives up rather than shrinking below the smallest side the server takes', async () => {
    stubPipeline({ width: 5000, height: 210, encodedSizes: [Math.round(VEHICLE_TARGET) + 100_000] })
    const original = fileOf(VEHICLE_IMAGE_CEILING.maxBytes + 1)

    await expect(withinCeiling(original, VEHICLE_IMAGE_CEILING)).resolves.toBe(original)
  })

  /** The decoded bitmap is released on every path, including the ones that fail. */
  it('releases the decoded image even when it gives up', async () => {
    const { closed } = stubPipeline({ width: 4000, height: 3000, encodedSizes: [] })

    await withinCeiling(fileOf(VEHICLE_IMAGE_CEILING.maxBytes + 1), VEHICLE_IMAGE_CEILING)

    expect(closed.count).toBe(1)
  })
})