import { Route, Routes } from 'react-router'

import { RequireAuth } from '../auth/RequireAuth.tsx'
import { AppLayout } from '../layouts/AppLayout.tsx'
import { RootLayout } from '../layouts/RootLayout.tsx'
import { AddVehiclePage } from '../pages/AddVehiclePage.tsx'
import { DashboardPage } from '../pages/DashboardPage.tsx'
import { ForgotPasswordPage } from '../pages/ForgotPasswordPage.tsx'
import { GaragePage } from '../pages/GaragePage.tsx'
import { LoginPage } from '../pages/LoginPage.tsx'
import { NotFoundPage } from '../pages/NotFoundPage.tsx'
import { ProfilePage } from '../pages/ProfilePage.tsx'
import { RegisterPage } from '../pages/RegisterPage.tsx'
import { ResetPasswordPage } from '../pages/ResetPasswordPage.tsx'
import { VerifyEmailPage } from '../pages/VerifyEmailPage.tsx'
import { WelcomePage } from '../pages/WelcomePage.tsx'
import { paths } from './paths.ts'

/**
 * The route table, deliberately separate from the Router that drives it.
 *
 * <p>Two branches: everything reachable without an account renders in
 * RootLayout, and everything behind RequireAuth renders in AppLayout with the
 * primary navigation. The not-found route lives in the public branch, so
 * somebody who mistyped an address is not asked to sign in first.
 */
export function AppRoutes() {
  return (
    <Routes>
      <Route element={<RootLayout />}>
        <Route path={paths.welcome} element={<WelcomePage />} />
        <Route path={paths.register} element={<RegisterPage />} />
        <Route path={paths.verifyEmail} element={<VerifyEmailPage />} />
        <Route path={paths.login} element={<LoginPage />} />
        <Route path={paths.forgotPassword} element={<ForgotPasswordPage />} />
        <Route path={paths.resetPassword} element={<ResetPasswordPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>

      <Route element={<RequireAuth />}>
        <Route element={<AppLayout />}>
          <Route path={paths.dashboard} element={<DashboardPage />} />
          <Route path={paths.garage} element={<GaragePage />} />
          <Route path={paths.addVehicle} element={<AddVehiclePage />} />
          <Route path={paths.profile} element={<ProfilePage />} />
        </Route>
      </Route>
    </Routes>
  )
}