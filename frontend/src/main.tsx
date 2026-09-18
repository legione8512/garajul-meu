import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import './i18n/config.ts'
import { AuthProvider } from './auth/AuthProvider.tsx'
import { lockPageScale } from './layouts/pageScale.ts'
import { showForegroundNotifications } from './notifications/foregroundNotifications.ts'
import { handleBackButton } from './routes/backButton.ts'
import App from './App.tsx'

// The native application only: a browser keeps pinch zoom, a browser's Back
// button belongs to the browser, and a browser receives no push. See
// pageScale.ts, backButton.ts and foregroundNotifications.ts.
if (import.meta.env.VITE_CLIENT === 'native') {
  lockPageScale(document)
  void handleBackButton()
  void showForegroundNotifications()
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AuthProvider>
      <App />
    </AuthProvider>
  </StrictMode>,
)
