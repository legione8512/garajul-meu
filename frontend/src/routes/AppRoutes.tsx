import { Route, Routes } from 'react-router'

import { RequireAuth } from '../auth/RequireAuth.tsx'
import { AppLayout } from '../layouts/AppLayout.tsx'
import { RootLayout } from '../layouts/RootLayout.tsx'
import { AddVehiclePage } from '../pages/AddVehiclePage.tsx'
import { CertificatePage } from '../pages/CertificatePage.tsx'
import { ChangeEmailPage } from '../pages/ChangeEmailPage.tsx'
import { ChangePasswordPage } from '../pages/ChangePasswordPage.tsx'
import { DashboardPage } from '../pages/DashboardPage.tsx'
import { DeleteAccountPage } from '../pages/DeleteAccountPage.tsx'
import { ForgotPasswordPage } from '../pages/ForgotPasswordPage.tsx'
import { FeaturesPage } from '../pages/FeaturesPage.tsx'
import { GaragePage } from '../pages/GaragePage.tsx'
import { LoginPage } from '../pages/LoginPage.tsx'
import { NotFoundPage } from '../pages/NotFoundPage.tsx'
import { PrivacyPage } from '../pages/PrivacyPage.tsx'
import { ProfilePage } from '../pages/ProfilePage.tsx'
import { RegisterPage } from '../pages/RegisterPage.tsx'
import { ResetPasswordPage } from '../pages/ResetPasswordPage.tsx'
import { TermsPage } from '../pages/TermsPage.tsx'
import { VehicleDetailsPage } from '../pages/VehicleDetailsPage.tsx'
import { VehicleDocumentDetailsPage } from '../pages/VehicleDocumentDetailsPage.tsx'
import { VehicleDocumentsPage } from '../pages/VehicleDocumentsPage.tsx'
import { VehicleHistoryPage } from '../pages/VehicleHistoryPage.tsx'
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
 *
 * <p>The legal pages are public, and that has a visible consequence worth
 * naming: a signed-in person who opens one leaves the authenticated frame and
 * loses the primary navigation until they go back. Registering them in both
 * branches would not help - two identical paths resolve to whichever is
 * declared first, so the public one would win regardless.
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
        <Route path={paths.terms} element={<TermsPage />} />
        <Route path={paths.privacy} element={<PrivacyPage />} />
        <Route path={paths.features} element={<FeaturesPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>

      <Route element={<RequireAuth />}>
        <Route element={<AppLayout />}>
          <Route path={paths.dashboard} element={<DashboardPage />} />
          <Route path={paths.garage} element={<GaragePage />} />
          <Route path={paths.addVehicle} element={<AddVehiclePage />} />
          <Route path={paths.vehiclePattern} element={<VehicleDetailsPage />} />
          <Route path={paths.certificatePattern} element={<CertificatePage />} />
          <Route path={paths.documentsPattern} element={<VehicleDocumentsPage />} />
          <Route path={paths.documentPattern} element={<VehicleDocumentDetailsPage />} />
          <Route path={paths.historyPattern} element={<VehicleHistoryPage />} />
          <Route path={paths.profile} element={<ProfilePage />} />
          <Route path={paths.changePassword} element={<ChangePasswordPage />} />
          <Route path={paths.changeEmail} element={<ChangeEmailPage />} />
          <Route path={paths.deleteAccount} element={<DeleteAccountPage />} />
        </Route>
      </Route>
    </Routes>
  )
}