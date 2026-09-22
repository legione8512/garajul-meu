import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import i18n from './i18n/config.ts'
import { AuthProvider } from './auth/AuthProvider.tsx'
import { lockPageScale } from './layouts/pageScale.ts'
import { watchConnectivity } from './network/connectivity.ts'
import { showForegroundNotifications } from './notifications/foregroundNotifications.ts'
import { watchReminderChannel } from './notifications/reminderChannel.ts'
import { handleBackButton } from './routes/backButton.ts'
import App from './App.tsx'

// The native application only: a browser keeps pinch zoom, a browser's Back
// button belongs to the browser, a browser receives no push, and a browser's
// own idea of the network is the only one it has. See pageScale.ts,
// backButton.ts, foregroundNotifications.ts and connectivity.ts - which must be
// started before the render below, so the first frame is not read from the
// WebView.
//
// The reminder channel before the foreground listener, never beside it: a
// notification posted to a channel that does not exist yet is dropped. See
// reminderChannel.ts.
if (import.meta.env.VITE_CLIENT === 'native') {
  lockPageScale(document)
  void watchConnectivity()
  void handleBackButton()
  void watchReminderChannel(i18n).then(showForegroundNotifications)
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AuthProvider>
      <App />
    </AuthProvider>
  </StrictMode>,
)
