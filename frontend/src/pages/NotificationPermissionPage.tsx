import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'

import { registerDevice } from '../api/endpoints/devices.ts'
import { FormError } from '../components/FormError.tsx'
import { useSubmission } from '../forms/useSubmission.ts'
import { push, type PushPermission } from '../notifications/push.ts'
import { paths } from '../routes/paths.ts'

/**
 * Screen 18 in specification section 5, and the one screen in the application
 * whose whole job is to be read before a button is pressed.
 *
 * <p><strong>Android raises its permission dialog once.</strong> After a refusal
 * the application can never raise it again - the only remaining route is the
 * phone's own settings, several taps away, where nobody goes. So the sentences
 * above the button are not courtesy: they are the single chance to explain what
 * is being asked for before the question is spent. Every layout decision here
 * follows from that, including the button being the last thing on the screen
 * rather than the first.
 *
 * <p>Three states, and each one is offered a different truthful thing. Still to
 * be asked: an explanation and a button. Granted: what will now happen, and
 * nothing to press. Refused: where the setting lives, said plainly, with no
 * button that would pretend the dialog can be raised again.
 *
 * <p><strong>The web is a fourth state and gets an honest answer rather than a
 * broken screen.</strong> Section 18 makes push native-only and V1 implements no
 * Firebase Web Push, so `push` is null in a browser and the route still exists -
 * somebody following a link from their phone's browser must be told why there is
 * nothing here rather than shown a control that cannot work.
 */
export function NotificationPermissionPage() {
  const { t } = useTranslation()
  const [permission, setPermission] = useState<PushPermission | null>(null)
  const enabling = useSubmission()

  useEffect(() => {
    if (push === null) {
      return
    }

    // Asks the operating system rather than remembering an answer: the
    // permission can be revoked in settings while the application is running,
    // and a remembered "granted" would then be a lie told by our own screen.
    void push.permission().then(setPermission)
  }, [])

  async function enable() {
    // A local binding, and the same one `PhotoChooser` needs for the same
    // reason: TypeScript will not carry a null check on an *imported* name into
    // a nested closure, because an ES module may reassign what it exports. The
    // registration below runs inside a callback, which is exactly such a
    // closure.
    const device = push

    if (device === null) {
      return
    }

    const answer = await device.request()
    setPermission(answer)

    if (answer !== 'granted') {
      return
    }

    // The registration is part of enabling, not a step after it. A permission
    // granted but never reported leaves the account with no device to send to,
    // which looks exactly like notifications being off.
    await enabling.submit(async () => {
      await registerDevice({
        platform: await device.platform(),
        pushToken: await device.token(),
        notificationsEnabled: true,
      })
    })
  }

  return (
    <>
      <h1>{t('screens.notifications')}</h1>

      <p><Link to={paths.profile}>{t('profile.back')}</Link></p>

      <p data-lead>{t('notifications.lead')}</p>
      <p>{t('notifications.what')}</p>

      {push === null && <p data-panel>{t('notifications.webOnly')}</p>}

      {permission === 'granted' && <p role="status">{t('notifications.granted')}</p>}

      {permission === 'denied' && <p data-panel>{t('notifications.denied')}</p>}

      <FormError error={enabling.error} />

      {permission === 'prompt' && (
        <p data-actions>
          <button
            type="button"
            disabled={enabling.pending}
            onClick={() => { void enable() }}
          >
            {t('notifications.enable')}
          </button>
        </p>
      )}
    </>
  )
}