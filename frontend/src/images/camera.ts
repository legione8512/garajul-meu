import { nativeCamera } from './nativeCamera.ts'

/**
 * A camera, where there is one. Specification section 13.
 *
 * <p>`null` on the web is the honest value, and the reason this is not an
 * interface with a refusing web implementation: a browser has no camera this
 * application can drive, so there is nothing to stand in for. `PhotoChooser`
 * reads the null and renders a file input instead - the control that has worked
 * on every platform since phase 8 and goes on working.
 *
 * <p>Chosen at build time by the same argument as `session/channel.ts`, and by
 * now the same evidence: a plugin's web implementation ships whatever the plugin
 * decided a browser should do, and that is not a decision this project delegates.
 */
export interface Camera {
  /**
   * A photograph taken now, or `null` if the person changed their mind.
   *
   * <p>`quality` differs per path and the two paths want opposite things - see
   * `nativeCamera.ts`, which holds the numbers and the reasoning.
   */
  takePhoto(quality: number): Promise<File | null>

  /** An existing picture, or `null` if the person changed their mind. */
  chooseFromGallery(): Promise<File | null>
}

export const camera: Camera | null =
  import.meta.env.VITE_CLIENT === 'native' ? nativeCamera : null

/**
 * Capture quality per path, kept beside the interface rather than beside the
 * implementation so a screen can name one without importing the native module.
 *
 * <p><strong>The certificate is captured at full quality.</strong> Document AI
 * cannot read the `I` family today, and the recorded trigger for revisiting that
 * is "a photograph at higher resolution reading `I` at all" - a scan degraded on
 * the way in would close off the one experiment that could settle it.
 *
 * <p><strong>The vehicle photograph is decorative, and 80 is plenty.</strong>
 * Not a limit: `withinCeiling` is the limit and applies to every upload whatever
 * its source. This only avoids capturing eight megabytes in order to re-encode
 * them a moment later.
 */
export const CERTIFICATE_SCAN_QUALITY = 100
export const VEHICLE_PHOTO_QUALITY = 80