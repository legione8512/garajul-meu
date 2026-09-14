import type { Camera } from './camera.ts'

/**
 * Capture quality per path, and the two paths want opposite things.
 *
 * <p><strong>The certificate is captured at full quality and never resized.</strong>
 * Document AI cannot read the `I` family today, and the recorded trigger for
 * revisiting that is "a photograph at higher resolution reading `I` at all" -
 * so a scan this application degraded on the way in would close off the one
 * experiment that could ever settle it.
 *
 * <p><strong>The vehicle photograph is decorative and captured at 80.</strong>
 * Not a limit - `withinCeiling` is the limit, and it is applied in
 * `endpoints/vehicles.ts` to every upload whatever its source. This is only
 * about not capturing an eight-megabyte JPEG in order to re-encode it a moment
 * later.
 *
 * <p>The same number goes to the gallery, where it is the quality of the JPEG
 * the picture is re-encoded into - see `chooseFromGallery` below for why a
 * picture from the gallery is re-encoded at all.
 *
 * <p><strong>No `targetWidth` or `targetHeight` on either path.</strong> The
 * plugin can resize, and `withinCeiling` already does. Two size limiters is two
 * places for the rules to disagree, and the one that exists is the one the
 * server's limits were written against.
 */

/**
 * Loaded inside the functions rather than at the top of the file, and the reason
 * was measured on 2026-08-31 with the secure-storage plugin: a static import of
 * a package that declares no `sideEffects` survives its importer being
 * tree-shaken, and `registerPlugin()` at module scope is such a side effect. The
 * web build dropped every line this project wrote and shipped the plugin anyway.
 * Inside a function there is no top-level import to survive.
 */

/**
 * The plugin answers with a `webPath` meant for an `<img src>`; everything
 * downstream of here - `withinCeiling`, the multipart body, the validator that
 * reads the magic bytes - speaks `File`. Fetching the path is how one becomes
 * the other, and it is a local read rather than a network request.
 *
 * <p>The name is arbitrary and never reaches the server as anything meaningful:
 * `VehicleImageValidator` takes the content type from the bytes, not from what
 * the client claimed.
 */
async function asFile(webPath: string | undefined): Promise<File | null> {
  if (webPath === undefined) {
    return null
  }

  const blob = await (await fetch(webPath)).blob()
  return new File([blob], 'photograph', { type: blob.type })
}

/**
 * A cancellation and a failure are not told apart, and that is a decision.
 *
 * <p>The plugin rejects for both, and matching on the message would tie this
 * file to wording the plugin is free to change. Cancelling is routine and
 * failing is not: with `saveToGallery` false the plugin asks for no Android
 * permission at all, so on a working device a rejection is somebody changing
 * their mind. Treating that as an error would put a red message on the screen
 * every time a person opened the camera and closed it again.
 *
 * <p>TRIGGER for revisiting: anybody reporting that the camera button does
 * nothing. That is what a genuine failure would look like from the outside, and
 * it is the one symptom this hides.
 */
async function attempt(take: () => Promise<string | undefined>): Promise<File | null> {
  try {
    return await asFile(await take())
  }
  catch {
    return null
  }
}

/**
 * The plugin is imported inside each call and used there. Returning it from an
 * `async` function would make the runtime ask a Capacitor Proxy for `.then`,
 * which it forwards to the platform as a native method - and the promise then
 * never settles. See `keystoreSecureStore.ts`, where that cost the first iOS run.
 */
export const nativeCamera: Camera = {
  takePhoto: quality => attempt(async () => {
    const { Camera: plugin } = await import('@capacitor/camera')
    const photo = await plugin.takePhoto({ quality, correctOrientation: true })
    return photo.webPath
  }),

  /**
   * <strong>Through the deprecated `getPhoto`, on purpose, because the
   * supported `chooseFromGallery` hands over the original file.</strong>
   *
   * <p>Found on 2026-09-14 on an iPad, where every photograph taken by an
   * iPhone or an iPad is HEIC. `chooseFromGallery` in plugin 8.2.4 answers with
   * the asset's own full-size file - ion-ios-camera 2.0.0 reads it through
   * `requestContentEditingInput` and `fullSizeImageURL` - so the HEIC reached the
   * server unchanged, and the server keeps JPEG and PNG only: the upload came
   * back IMAGE_INVALID_TYPE. The same happens on an iPhone with any photograph
   * its own camera took; the iPhone run on 2026-09-11 had simply picked a
   * picture that was already a JPEG.
   *
   * <p>`getPhoto` with `CameraSource.Photos` decodes the chosen picture and
   * writes a JPEG at `quality`, whatever the picture was. Read in the plugin on
   * 2026-09-14: on iOS it is `PHPickerViewController`, a `UIImage` and
   * `generateJPEG`; on Android it is the system photo picker, a `Bitmap` and
   * `Bitmap.CompressFormat.JPEG`. `correctOrientation` draws the image upright,
   * which the certificate scan needs anyway.
   *
   * <p>The alternatives, and why not. Re-encoding in the page needs the WebView
   * to decode HEIC first, which is not something to count on in WebKit on
   * iPadOS 16 or in Android's WebView. Accepting HEIC on the server needs a
   * decoder Java does not ship, and a format policy the specification does not
   * have.
   *
   * <p>What it costs, stated so nobody rediscovers it:
   * - It is deprecated. It works in Capacitor 8 and may not exist in 9.
   * - It still asks for photo-library access before it opens the picker, even
   *   though the picker itself would not need it.
   * - On iOS it refuses to open at all unless Info.plist declares
   *   NSPhotoLibraryAddUsageDescription, a key for writing to the library that
   *   this application never does. `attempt` above would turn that refusal
   *   into a button that does nothing, which is why Info.plist declares it and
   *   says why.
   *
   * <p>TRIGGER for returning to `chooseFromGallery`: the Capacitor 9 upgrade,
   * or ion-ios-camera handing back a JPEG for a HEIC asset - check by choosing a
   * photograph an iPhone's camera took and uploading it as the vehicle photo.
   */
  chooseFromGallery: quality => attempt(async () => {
    const { Camera: plugin, CameraResultType, CameraSource } = await import('@capacitor/camera')
    const photo = await plugin.getPhoto({
      source: CameraSource.Photos,
      resultType: CameraResultType.Uri,
      quality,
      correctOrientation: true,
    })
    return photo.webPath
  }),
}