import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'

import { paths } from '../routes/paths.ts'
import { push } from './push.ts'

/**
 * What this client can and cannot do about notifications, said in one place.
 *
 * <p>Two screens showed the same sentence and it was about to become false on
 * both. `reminders.nativeOnly` said push arrives in "the phone application,
 * which does not exist yet" - true when it was written, a lie the day the
 * Android application ships, and doubly a lie when read *inside* that
 * application, where notifications arrive here.
 *
 * <p>So the note is a component rather than a string. In a browser it says push
 * is native-only, which stays true and no longer claims the app is unbuilt. On a
 * phone it points at screen 18, which is the only place the answer can actually
 * be changed.
 */
export function PushChannelNote() {
  const { t } = useTranslation()

  if (push === null) {
    return <p>{t('reminders.webOnly')}</p>
  }

  return (
    <p>
      {t('reminders.onThisPhone')}
      {' '}
      <Link to={paths.notifications}>{t('reminders.manage')}</Link>
    </p>
  )
}