import { useCallback, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'

import { FormError } from '../components/FormError.tsx'
import { useSubmission } from '../forms/useSubmission.ts'
import { push, type PushPermission } from '../notifications/push.ts'
import { reportDevice } from '../notifications/reportDevice.ts'
import { paths } from '../routes/paths.ts'
import { NotificationPreferences } from '../settings/NotificationPreferences.tsx'

/**
 * Why the registration is running. It changes only the sentence shown while it
 * runs: checking on arrival, enabling after the button or a retry.
 */
type Purpose = 'checking' | 'enabling'

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
 * be asked: an explanation and a button. Refused: where the setting lives, said
 * plainly, with no button that would pretend the dialog can be raised again.
 * Granted: <strong>what the account holds, not what the operating system
 * allows</strong>.
 *
 * <p><strong>Granted changed on 2026-09-11.</strong> Until then this screen said
 * "Notificările sunt activate" the moment the dialog was answered, before the
 * registration had even started, and went on saying it when the registration
 * failed or never finished - so an account with no device to send to looked
 * exactly like one with notifications on, and nothing anywhere said otherwise.
 * Now granted is one of three sentences: checking or enabling while
 * `reportDevice` runs, "activate" only when the server's answer holds this phone
 * as able, and otherwise that notifications are not on yet, the reason, and a
 * way to try again.
 *
 * <p><strong>The account's preferences live here too, since the same day</strong>
 * - moved from screen 15 by the developer's decision, against the
 * specification's screen map, after screen 15 showed "Trimite-mi notificări"
 * ticked beside a phone that refused notifications. They appear once the phone
 * is held, and in a browser; never on a phone that refuses or has not been asked,
 * where nothing set there could reach it. "Activate" depends on the account
 * sending anything as well, so that sentence belongs to `NotificationPreferences`.
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
  const [purpose, setPurpose] = useState<Purpose>('checking')
  const [held, setHeld] = useState(false)
  const { pending, error, submit } = useSubmission()

  /**
   * Registers this phone and believes only the server's answer. It is the same
   * report the launch sends, and `reportDevice` shares one already in progress
   * rather than racing it, so this screen and the account cannot disagree about
   * what was sent.
   */
  const confirm = useCallback(async (why: Purpose) => {
    setPurpose(why)
    setHeld(false)

    await submit(async () => {
      const device = await reportDevice()
      setHeld(device !== null && device.notificationsEnabled)
    })
  }, [submit])

  useEffect(() => {
    if (push === null) {
      return
    }

    // Asks the operating system rather than remembering an answer: the
    // permission can be revoked in settings while the application is running,
    // and a remembered "granted" would then be a lie told by our own screen.
    void push.permission().then((answer) => {
      setPermission(answer)

      if (answer === 'granted') {
        void confirm('checking')
      }
    })
  }, [confirm])

  async function enable() {
    // A local binding, and the same one `PhotoChooser` needs for the same
    // reason: TypeScript will not carry a null check on an *imported* name into
    // a nested closure, because an ES module may reassign what it exports.
    const device = push

    if (device === null) {
      return
    }

    const answer = await device.request()
    setPermission(answer)

    // The registration is part of enabling, not a step after it. A permission
    // granted but never reported leaves the account with no device to send to,
    // which looks exactly like notifications being off.
    if (answer === 'granted') {
      await confirm('enabling')
    }
  }

  const granted = permission === 'granted'

  return (
    <>
      <h1>{t('screens.notifications')}</h1>

      <p><Link to={paths.profile}>{t('profile.back')}</Link></p>

      <p data-lead>{t('notifications.lead')}</p>
      <p>{t('notifications.what')}</p>

      {push === null && <p data-panel>{t('notifications.webOnly')}</p>}

      {push === null && <NotificationPreferences onThisPhone={false} />}

      {granted && pending && (
        <p role="status">
          {purpose === 'enabling' ? t('notifications.enabling') : t('notifications.checking')}
        </p>
      )}

      {granted && !pending && held && <NotificationPreferences onThisPhone />}

      {granted && !pending && !held && (
        <>
          <p data-panel>{t('notifications.notActive')}</p>
          <FormError error={error} />
          <p data-actions>
            <button type="button" onClick={() => { void confirm('enabling') }}>
              {t('common.retry')}
            </button>
          </p>
        </>
      )}

      {permission === 'denied' && <p data-panel>{t('notifications.denied')}</p>}

      {permission === 'prompt' && (
        <p data-actions>
          <button
            type="button"
            disabled={pending}
            onClick={() => { void enable() }}
          >
            {t('notifications.enable')}
          </button>
        </p>
      )}
    </>
  )
}
