/**
 * Making an over-sized photograph fit before it is sent.
 *
 * <p><strong>Only when it would be refused anyway.</strong> A file already
 * within the ceiling is returned untouched, with no decode and no re-encode, so
 * nothing that was going to work is made worse. That rule is what lets the
 * certificate scan keep every pixel it has: the `I` family is unreadable to
 * Document AI today and the recorded trigger for revisiting it is a photograph
 * at *higher* resolution, so shrinking a scan that would have been accepted
 * would close off the one thing that could ever settle it.
 *
 * <p><strong>Best effort, never a gate.</strong> Any failure - a browser with no
 * canvas, a file the decoder refuses, a codec that returns nothing - returns the
 * original file. The server enforces the real limit and answers with a
 * translated code; this only removes a rejection the client could see coming.
 * jsdom has no `createImageBitmap` at all, which is why the tests exercise that
 * path deliberately rather than by accident.
 */

/** What the server refuses above. Mirrors `garajul-meu.storage` and `.ocr`. */
export interface Ceiling {
  readonly maxBytes: number
  readonly maxPixels: number
}

/**
 * The two ceilings, and they are copies of numbers the backend owns.
 *
 * <p>A second source of truth, stated rather than hidden. It is safe in one
 * direction only: these may drift *below* what the server allows with no effect
 * beyond a slightly smaller upload, because the server is still the one that
 * refuses. Drift the other way - the server lowering its limit - simply returns
 * the behaviour we have today, which is an honest rejection. TRIGGER: any change
 * to `max-upload-bytes` or `max-pixels` in `application.yml`.
 */
export const VEHICLE_IMAGE_CEILING: Ceiling = { maxBytes: 5_242_880, maxPixels: 40_000_000 }
export const CERTIFICATE_SCAN_CEILING: Ceiling = { maxBytes: 10_485_760, maxPixels: 40_000_000 }

/** Aim comfortably under, so a re-encode that lands near the line still passes. */
const TARGET_FRACTION = 0.8

const QUALITY = 0.85

/** Each pass takes the linear dimensions to 70%, so four passes reach a quarter. */
const SHRINK_STEP = 0.7
const MAX_PASSES = 4

/**
 * The server's own `min-side`. Shrinking past it would trade one rejection for
 * another, so the original goes instead and the refusal at least names the real
 * problem.
 */
const MIN_SIDE = 200

export async function withinCeiling(file: File, ceiling: Ceiling): Promise<File> {
  if (file.size <= ceiling.maxBytes) {
    return file
  }

  try {
    return await shrink(file, ceiling)
  } catch {
    // Deliberately silent and deliberately broad. Nothing here is worth failing
    // an upload over: the worst case is the request the client would have made
    // anyway.
    return file
  }
}

async function shrink(file: File, ceiling: Ceiling): Promise<File> {
  // `from-image` matters more than it looks. A photograph from a phone is very
  // often stored sideways with an EXIF rotation, and a canvas draw that ignores
  // it silently produces a rotated certificate - which the OCR would then read
  // at ninety degrees.
  const bitmap = await createImageBitmap(file, { imageOrientation: 'from-image' })

  try {
    const target = ceiling.maxBytes * TARGET_FRACTION
    const pixels = bitmap.width * bitmap.height

    // First pass already satisfies the pixel ceiling; the loop then deals with
    // bytes, which no formula predicts from pixels.
    let scale = Math.min(1, Math.sqrt(ceiling.maxPixels / pixels))

    for (let pass = 0; pass < MAX_PASSES; pass++) {
      if (Math.min(bitmap.width, bitmap.height) * scale < MIN_SIDE) {
        return file
      }

      const encoded = await encode(bitmap, scale)
      if (encoded === null) {
        return file
      }
      if (encoded.size <= target) {
        return new File([encoded], asJpeg(file.name), {
          type: 'image/jpeg',
          lastModified: file.lastModified,
        })
      }

      scale *= SHRINK_STEP
    }

    return file
  } finally {
    bitmap.close()
  }
}

async function encode(bitmap: ImageBitmap, scale: number): Promise<Blob | null> {
  const canvas = document.createElement('canvas')
  canvas.width = Math.max(1, Math.round(bitmap.width * scale))
  canvas.height = Math.max(1, Math.round(bitmap.height * scale))

  const context = canvas.getContext('2d')
  if (context === null) {
    return null
  }

  context.drawImage(bitmap, 0, 0, canvas.width, canvas.height)

  return await new Promise<Blob | null>((resolve) => {
    canvas.toBlob(resolve, 'image/jpeg', QUALITY)
  })
}

/** The bytes are JPEG now whatever they were, and the name should not lie. */
function asJpeg(name: string): string {
  return `${name.replace(/\.[^.]+$/, '')}.jpg`
}