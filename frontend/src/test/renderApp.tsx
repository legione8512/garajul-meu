import { render } from '@testing-library/react'
import { MemoryRouter } from 'react-router'

import { AuthProvider } from '../auth/AuthProvider.tsx'
import { AppRoutes } from '../routes/AppRoutes.tsx'

/**
 * Renders the real route table at a chosen address, inside the real provider.
 *
 * <p>Deliberately not a shallow render of one page: the pages read the
 * authentication context and navigate between themselves, and testing either of
 * those through a mock would only prove the mock works.
 *
 * @param state what a previous screen would have handed over, for the screens
 *              that are normally reached from another one
 */
export function renderApp(path: string, state?: unknown) {
  return render(
    <MemoryRouter initialEntries={[{ pathname: path, state }]}>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </MemoryRouter>,
  )
}