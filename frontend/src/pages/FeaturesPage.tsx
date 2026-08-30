import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'

import { paths } from '../routes/paths.ts'

/**
 * What the application does, for somebody who has not signed up.
 *
 * <p><strong>Not one of section 5's twenty-two screens.</strong> Added on
 * 2026-08-29 with the owner's approval, and recorded as the deviation it is.
 * The reasoning is the one the legal pages already rest on: a person deciding
 * whether to create an account cannot find out what they would be creating it
 * for from inside it.
 *
 * <p>Sections and prose rather than the landing page's cards. Cards are for
 * skimming and this page is for reading - somebody who opened it has already
 * decided to spend a minute.
 *
 * <p><strong>Every claim here is one the application can keep today.</strong>
 * Push notifications are deliberately absent: section 18 makes them Android and
 * iOS only and the applications do not exist yet, so the reminder section says
 * what is prepared rather than what is delivered. A features page that promises
 * a notification nobody receives is the fastest way to teach somebody that the
 * rest of it is also untrue.
 */
export function FeaturesPage() {
  const { t } = useTranslation()

  return (
    <>
      <h1>{t('screens.features')}</h1>

      <p><Link to={paths.welcome}>{t('common.backToStart')}</Link></p>

      <p data-lead>{t('features.lead')}</p>

      <section>
        <h2>{t('features.garage.title')}</h2>
        <p>{t('features.garage.body')}</p>
      </section>

      <section>
        <h2>{t('features.certificate.title')}</h2>
        <p>{t('features.certificate.body')}</p>
      </section>

      <section>
        <h2>{t('features.documents.title')}</h2>
        <p>{t('features.documents.body')}</p>
      </section>

      <section>
        <h2>{t('features.dashboard.title')}</h2>
        <p>{t('features.dashboard.body')}</p>
      </section>

      <section>
        <h2>{t('features.history.title')}</h2>
        <p>{t('features.history.body')}</p>
      </section>

      <section>
        <h2>{t('features.reminders.title')}</h2>
        <p>{t('features.reminders.body')}</p>
      </section>

      <section>
        <h2>{t('features.account.title')}</h2>
        <p>{t('features.account.body')}</p>
      </section>

      <p data-actions>
        <Link data-action="primary" to={paths.register}>
          {t('welcome.createAccount')}
        </Link>
        <Link data-action="secondary" to={paths.login}>
          {t('welcome.signIn')}
        </Link>
      </p>
    </>
  )
}