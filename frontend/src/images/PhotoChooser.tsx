import { useId } from 'react'
import { useTranslation } from 'react-i18next'

import { camera } from './camera.ts'

interface PhotoChooserProps {
  /** What this control offers to do, in the words of the screen it sits on. */
  label: string
  /** The capture quality for the camera path. Ignored where there is no camera. */
  quality: number
  disabled?: boolean
  onChosen: (file: File) => void
}

/**
 * One photograph, however this client gets one.
 *
 * <p>Three screens took a photograph through their own `<input type="file">`
 * and each cleared `input.value` afterwards - one of them explaining why and two
 * of them not. That idiom is the reason this component exists as much as the
 * camera is: **a file input fires no change event when the selection has not
 * changed**, so without the reset a second attempt at the same photograph, after
 * a scan that failed, does nothing at all.
 *
 * <p><strong>The web path is unchanged, deliberately.</strong> Same hidden
 * input, same styled label, same `data-file` pair the stylesheet already knows -
 * a control that has worked since phase 8 and works in mobile browsers too,
 * which is most of the people who will ever use this.
 *
 * <p>Where there is a camera the choice is two buttons rather than the plugin's
 * own dialog. Version 8.1.0 replaced `getPhoto` and its `CameraSource.Prompt`
 * with separate `takePhoto` and `chooseFromGallery` methods, so the application
 * asks the question itself - which it would have wanted anyway, since the
 * plugin's dialog needed its labels translated through `promptLabel*` to say
 * what two ordinary buttons say for free.
 *
 * <p>Both buttons are quiet. They replace a control that was never a screen's
 * primary action, and making them louder than the Save beneath them would say
 * something false about what the screen is for.
 */
export function PhotoChooser({ label, quality, disabled, onChosen }: PhotoChooserProps) {
  const { t } = useTranslation()
  const inputId = useId()

  // A local binding, and not a redundant one. TypeScript will not carry the
  // null check on an *imported* name into a callback body - an ES module may
  // reassign what it exports, so a live binding could change between the test
  // and the call. It cannot happen here, and the compiler is right to refuse to
  // assume so. Narrowing a local const does survive into the closures below.
  const device = camera

  async function fromCamera(take: () => Promise<File | null>) {
    const file = await take()

    if (file !== null) {
      onChosen(file)
    }
  }

  if (device !== null) {
    return (
      <p data-actions>
        <button
          data-quiet
          type="button"
          disabled={disabled}
          onClick={() => { void fromCamera(() => device.takePhoto(quality)) }}
        >
          {t('photo.take')}
        </button>
        <button
          data-quiet
          type="button"
          disabled={disabled}
          onClick={() => { void fromCamera(() => device.chooseFromGallery()) }}
        >
          {t('photo.fromGallery')}
        </button>
      </p>
    )
  }

  return (
    <>
      <input
        data-file
        id={inputId}
        type="file"
        accept="image/jpeg,image/png"
        disabled={disabled}
        onChange={(event) => {
          const file = event.target.files?.[0]

          // See the note above: this is what lets the same photograph be tried
          // twice after a failure.
          event.target.value = ''

          if (file !== undefined) {
            onChosen(file)
          }
        }}
      />
      <label data-file htmlFor={inputId}>{label}</label>
    </>
  )
}