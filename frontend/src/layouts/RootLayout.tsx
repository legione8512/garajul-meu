import { Outlet } from 'react-router'

import { SiteFooter } from '../components/SiteFooter.tsx'
import { SiteHeader } from '../components/SiteHeader.tsx'
import { OfflineNotice } from '../network/OfflineNotice.tsx'

/**
 * The public frame. Everything reachable without an account renders inside this.
 *
 * <p>The offline band sits above the content rather than inside it, so it is the
 * same band on every screen and no page has to remember to render it.
 */
export function RootLayout() {
  return (
    <>
      <SiteHeader />
      <OfflineNotice />
      <main>
        <Outlet />
      </main>
      <SiteFooter />
    </>
  )
}