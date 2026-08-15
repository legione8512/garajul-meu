import { Outlet } from 'react-router'

import { SiteHeader } from '../components/SiteHeader.tsx'

/**
 * The public frame: header and content, nothing else. Everything reachable
 * without an account renders inside this.
 */
export function RootLayout() {
  return (
    <>
      <SiteHeader />
      <main>
        <Outlet />
      </main>
    </>
  )
}