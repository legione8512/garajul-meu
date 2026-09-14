import { beforeEach, describe, expect, it, vi } from 'vitest'

/**
 * The plugin's methods, replaced; its enums, kept.
 *
 * <p>The enums come from the real package so that the assertions below compare
 * against the values the native side actually switches on. A mock spelling
 * `'PHOTOS'` itself would agree with a test spelling `'PHOTOS'` whatever the
 * plugin expects.
 */
const plugin = vi.hoisted(() => ({
  takePhoto: vi.fn(),
  chooseFromGallery: vi.fn(),
  getPhoto: vi.fn(),
}))

vi.mock('@capacitor/camera', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@capacitor/camera')>()
  return {
    CameraResultType: actual.CameraResultType,
    CameraSource: actual.CameraSource,
    Camera: plugin,
  }
})

import { CameraResultType, CameraSource } from '@capacitor/camera'

import { nativeCamera } from './nativeCamera.ts'

const JPEG = new Uint8Array([0xff, 0xd8, 0xff, 0xe0])

/**
 * What reading a `webPath` answers inside the WebView: the bytes the plugin wrote.
 *
 * <p>Only `blob()`, and not a real `Response`. Node's `Response` hands back
 * Node's `Blob`, which jsdom's `File` does not recognise and stringifies into
 * the fifteen bytes of "[object Blob]" - a failure of the test's two realms that
 * a WebView, where fetch and File share one, cannot have.
 */
function stubReadingTheFile() {
  const read = vi.fn(() => Promise.resolve({
    blob: () => Promise.resolve(new Blob([JPEG], { type: 'image/jpeg' })),
  }))
  vi.stubGlobal('fetch', read)
  return read
}

beforeEach(() => {
  plugin.takePhoto.mockReset()
  plugin.chooseFromGallery.mockReset()
  plugin.getPhoto.mockReset()
})

describe('nativeCamera.chooseFromGallery', () => {
  it('asks the plugin for a re-encoded JPEG from the photo library, at the quality it was given', async () => {
    plugin.getPhoto.mockResolvedValue({ webPath: 'capacitor://localhost/_capacitor_file_/photo-1.jpg', format: 'jpeg' })
    stubReadingTheFile()

    await nativeCamera.chooseFromGallery(80)

    expect(plugin.getPhoto).toHaveBeenCalledWith({
      source: CameraSource.Photos,
      resultType: CameraResultType.Uri,
      quality: 80,
      correctOrientation: true,
    })
  })

  // The regression itself: chooseFromGallery hands over the original file, and
  // from an iPhone or an iPad that is HEIC, which the server refuses.
  it('never uses chooseFromGallery, which hands over the original file', async () => {
    plugin.getPhoto.mockResolvedValue({ webPath: 'capacitor://localhost/_capacitor_file_/photo-1.jpg', format: 'jpeg' })
    stubReadingTheFile()

    await nativeCamera.chooseFromGallery(100)

    expect(plugin.chooseFromGallery).not.toHaveBeenCalled()
  })

  it('answers with the bytes the plugin wrote, read from its webPath', async () => {
    const webPath = 'capacitor://localhost/_capacitor_file_/photo-1.jpg'
    plugin.getPhoto.mockResolvedValue({ webPath, format: 'jpeg' })
    const read = stubReadingTheFile()

    const file = await nativeCamera.chooseFromGallery(100)

    expect(read).toHaveBeenCalledWith(webPath)
    expect(file).toBeInstanceOf(File)
    expect(new Uint8Array(await (file as File).arrayBuffer())).toEqual(JPEG)
    expect(file?.type).toBe('image/jpeg')
  })

  it('answers null when the person closes the picker', async () => {
    plugin.getPhoto.mockRejectedValue(new Error('User cancelled photos app'))
    const read = stubReadingTheFile()

    await expect(nativeCamera.chooseFromGallery(80)).resolves.toBeNull()
    expect(read).not.toHaveBeenCalled()
  })
})

describe('nativeCamera.takePhoto', () => {
  it('is unchanged: the camera path still uses takePhoto', async () => {
    plugin.takePhoto.mockResolvedValue({ webPath: 'capacitor://localhost/_capacitor_file_/photo-2.jpg' })
    stubReadingTheFile()

    await nativeCamera.takePhoto(100)

    expect(plugin.takePhoto).toHaveBeenCalledWith({ quality: 100, correctOrientation: true })
    expect(plugin.getPhoto).not.toHaveBeenCalled()
  })
})