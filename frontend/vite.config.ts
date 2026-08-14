import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  test: {
    // jsdom rather than the default node environment: these are component
    // tests and they need a DOM to render into.
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    // Globals stay off. Importing describe/it/expect costs one line per file
    // and keeps the test API visible instead of ambient - the same reason the
    // backend never relies on static imports nobody can trace.
    globals: false,
    restoreMocks: true,
  },
})