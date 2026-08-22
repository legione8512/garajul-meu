import { defineConfig, devices } from '@playwright/test'

/**
 * The end-to-end suite. Specification section 28 names one flow - register,
 * verify, login, vehicle, certificate, document - and that is what this runs.
 *
 * <p>Playwright starts Vite and nothing else. Starting the backend from here as
 * well would couple two build systems and produce failures nobody can read: a
 * Maven stack trace arriving inside a browser test report tells you neither
 * which one broke nor why. The backend is a stated prerequisite instead, checked
 * in globalSetup with a message that says exactly what to run.
 *
 * <p>Romanian locale on purpose. It is the reference locale, the one every
 * assertion in this suite reads its wording from, and the language the
 * application defaults to for anybody who has not asked otherwise.
 */
export default defineConfig({
  testDir: './e2e',
  globalSetup: './e2e/global-setup.ts',
  // One worker, and no retries. These tests share one backend and one database;
  // running them in parallel would let one journey's account collide with
  // another's, and a retry would hide a flake that is worth seeing.
  workers: 1,
  retries: 0,
  timeout: 60_000,
  reporter: process.env.CI ? 'list' : 'html',
  use: {
    baseURL: 'http://localhost:5173',
    locale: 'ro-RO',
    trace: 'retain-on-failure',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:5173',
    reuseExistingServer: !process.env.CI,
    timeout: 60_000,
  },
})