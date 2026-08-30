import { useTranslation } from 'react-i18next'
import { Link, Navigate } from 'react-router'

import heroCar from '../assets/hero-car.webp'
import { useAuth } from '../auth/useAuth.ts'
import { paths } from '../routes/paths.ts'

/**
 * Screen 1 in specification section 5: the public landing page.
 *
 * <p>Somebody already signed in has no use for it, so they are sent to the
 * dashboard. Keeping `/` public and `/dashboard` separate, rather than having
 * one address render two different pages, costs a redirect on arrival and buys
 * routes that can be read, tested and sent to somebody as a link.
 *
 * <p>While the status is unknown neither branch shows: offering "sign in" for
 * that moment would tell somebody who is signed in that they are not.
 *
 * <p><strong>The car carries `alt=""` on purpose.</strong> It is the product's
 * mark and says nothing the heading beneath it does not say in words, so
 * describing it again would make a screen reader announce the same thing twice.
 * An empty alt is the instruction to skip it - which is different from having no
 * alt at all, where the file name gets read out instead.
 *
 * <p>The image is opaque and its own background is the page's background, taken
 * from the file rather than guessed: `#151321` is the most common colour on its
 * border ring, 284 of 3,972 pixels, with the next three within two points. The
 * seam is invisible and the lighter vignette reads as the glow it is.
 *
 * <p>`width` and `height` are the file's real dimensions and are here to reserve
 * the space before the bytes arrive. Without them the heading and the two links
 * jump downward on load, which is how somebody ends up clicking the wrong one.
 */
export function WelcomePage() {
  const { t } = useTranslation()
  const { status } = useAuth()

  if (status === 'authenticated') {
    return <Navigate to={paths.dashboard} replace />
  }

  return (
    <>
      <img data-hero src={heroCar} alt="" width={1254} height={732} />

      <h1>{t('welcome.headline')}</h1>
      <p data-lead>{t('welcome.lead')}</p>

      {status === 'anonymous' ? (
        <p data-actions>
          <Link data-action="primary" to={paths.register}>
            {t('welcome.createAccount')}
          </Link>
          <Link data-action="secondary" to={paths.login}>
            {t('welcome.signIn')}
          </Link>
        </p>
      ) : null}

      {/*
        A list because it is one: three independent claims in no particular
        order. Each carries its own heading so the page can be skimmed by
        somebody reading it with their eyes and traversed by somebody reading it
        with anything else.
      */}
      <p><Link to={paths.features}>{t('features.readMore')}</Link></p>

      <ul data-features>
        <li>
          <h2>{t('welcome.features.scanTitle')}</h2>
          <p>{t('welcome.features.scanBody')}</p>
        </li>
        <li>
          <h2>{t('welcome.features.documentsTitle')}</h2>
          <p>{t('welcome.features.documentsBody')}</p>
        </li>
        <li>
          <h2>{t('welcome.features.remindersTitle')}</h2>
          <p>{t('welcome.features.remindersBody')}</p>
        </li>
      </ul>
    </>
  )
}
