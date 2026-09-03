import { useTranslation } from 'react-i18next'
import { NavLink, Outlet } from 'react-router'

import { SiteFooter } from '../components/SiteFooter.tsx'
import { SiteHeader } from '../components/SiteHeader.tsx'
import { OfflineNotice } from '../network/OfflineNotice.tsx'
import { paths } from '../routes/paths.ts'
import { useDeviceRegistration } from '../notifications/useDeviceRegistration.ts'

/**
 * The authenticated frame. Section 5 fixes the primary navigation at exactly
 * three destinations - Home, Garage, Profile - with supporting flows reached
 * contextually rather than as further tabs.
 *
 * <p>The nav is a sibling of main, not a child of it, so assistive technology
 * can jump to it as navigation. And NavLink rather than Link because it sets
 * aria-current="page" on the active destination by itself: without that a
 * screen reader announces three identical links and never says which page the
 * reader is on.
 */
export function AppLayout() {
  const { t } = useTranslation()

  // Here rather than in AuthProvider, which is about who is signed in and not
  // about phones. This frame renders only behind RequireAuth and stays mounted
  // while the screens inside it change, so "once per launch" is what it means
  // without anybody having to remember it. A no-op on the web.
  useDeviceRegistration()

  return (
    <>
      <SiteHeader />
      <OfflineNotice />
      <nav aria-label={t('navigation.label')}>
        <NavLink to={paths.dashboard}>{t('navigation.home')}</NavLink>
        {' '}
        <NavLink to={paths.garage}>{t('navigation.garage')}</NavLink>
        {' '}
        <NavLink to={paths.profile}>{t('navigation.profile')}</NavLink>
      </nav>
      <main>
        <Outlet />
      </main>
      <SiteFooter />
    </>
  )
}