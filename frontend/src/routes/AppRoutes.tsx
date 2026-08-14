import { Route, Routes } from 'react-router'

import { RootLayout } from '../layouts/RootLayout.tsx'
import { ForgotPasswordPage } from '../pages/ForgotPasswordPage.tsx'
import { LoginPage } from '../pages/LoginPage.tsx'
import { NotFoundPage } from '../pages/NotFoundPage.tsx'
import { RegisterPage } from '../pages/RegisterPage.tsx'
import { ResetPasswordPage } from '../pages/ResetPasswordPage.tsx'
import { VerifyEmailPage } from '../pages/VerifyEmailPage.tsx'
import { WelcomePage } from '../pages/WelcomePage.tsx'
import { paths } from './paths.ts'

/**
 * The route table, deliberately separate from the Router that drives it.
 *
 * The layout route has no path of its own, so RootLayout wraps every page
 * including the not-found one - which is precisely where somebody lost needs a
 * way back.
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
    </Routes>
  )
}