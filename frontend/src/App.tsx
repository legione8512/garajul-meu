import { BrowserRouter } from 'react-router'

import { AppRoutes } from './routes/AppRoutes.tsx'

/**
 * Only the Router lives here. Everything testable lives in AppRoutes, because
 * BrowserRouter reads the real address bar and cannot be told to start
 * elsewhere.
 */
export default function App() {
  return (
    <BrowserRouter>
      <AppRoutes />
    </BrowserRouter>
  )
}