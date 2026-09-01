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
async function plugin() {
  const { Camera } = await import('@capacitor/camera')
  return Camera
}

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

export const nativeCamera: Camera = {
  takePhoto: quality => attempt(async () => {
    const photo = await (await plugin()).takePhoto({ quality, correctOrientation: true })
    return photo.webPath
  }),

  chooseFromGallery: () => attempt(async () => {
    const chosen = await (await plugin()).chooseFromGallery({ allowMultipleSelection: false })
    return chosen.results[0]?.webPath
  }),
}