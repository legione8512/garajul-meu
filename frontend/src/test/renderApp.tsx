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
 */
export function renderApp(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </MemoryRouter>,
  )
}