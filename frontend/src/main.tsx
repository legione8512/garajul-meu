import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import './i18n/config.ts'
import { AuthProvider } from './auth/AuthProvider.tsx'
import { lockPageScale } from './layouts/pageScale.ts'
import App from './App.tsx'

// The native application only: a browser keeps pinch zoom. See pageScale.ts.
if (import.meta.env.VITE_CLIENT === 'native') {
  lockPageScale(document)
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AuthProvider>
      <App />
    </AuthProvider>
  </StrictMode>,
)