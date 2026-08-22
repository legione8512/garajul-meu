import { expect, test } from '@playwright/test'

import { ro } from '../src/i18n/locales/ro.ts'
import { waitForCode } from './mailbox.ts'

/**
 * The one flow specification section 28 names: register → verify → login →
 * vehicle → certificate → document.
 *
 * <p>Written as a single test rather than six, because it is one journey and
 * every step depends on the one before it. Split into separate tests they would
 * either share mutable state between tests - which Playwright rightly makes
 * awkward - or each repeat the whole prefix, turning a thirty-second suite into
 * a three-minute one to assert the same things.
 *
 * <p>What this covers that nothing else does is the seam: two applications, two
 * processes, a real browser and a real database, with CORS, cookies, tokens and
 * navigation all in play at once. Every one of those has already broken
 * something in this project that unit tests called green - most recently the
 * missing PUT method, which cost two phases.
 *
 * <p><strong>The navigation links are matched exactly, because the application's
 * own name contains one of them.</strong> Playwright matches an accessible name
 * by substring unless told otherwise, so "Garaj" also finds "Garajul Meu" in the
 * header and the query resolves to two links. That is not a quirk of this test:
 * any query for that destination has the same problem, for as long as the
 * product is called what it is called.
 *
 * <p>The address is unique per run. The database is thrown away each time, so
 * strictly it need not be; it is, so that pointing this suite at a persistent
 * database during debugging does not fail on the second run for a reason that
 * has nothing to do with the code.
 */
test('a new account reaches a vehicle, its certificate and a document', async ({ page }) => {
  const email = `e2e-${String(Date.now())}@example.com`
  const password = 'o-parola-de-test-lunga'

  await test.step('register', async () => {
    await page.goto('/')
    await page.getByRole('link', { name: ro.welcome.createAccount }).click()

    await page.getByLabel(ro.fields.fullName).fill('Marius E2E')
    await page.getByLabel(ro.fields.email).fill(email)
    await page.getByLabel(ro.fields.password, { exact: true }).fill(password)
    await page.getByRole('button', { name: ro.register.submit }).click()

    await expect(
      page.getByRole('heading', { level: 1, name: ro.screens.verifyEmail }),
    ).toBeVisible()
  })

  await test.step('verify the address with the code that was actually sent', async () => {
    const code = await waitForCode('verification', email)

    await page.getByLabel(ro.fields.code).fill(code)
    await page.getByRole('button', { name: ro.verifyEmail.submit }).click()

    await expect(page.getByRole('heading', { level: 1, name: ro.screens.login })).toBeVisible()
  })

  await test.step('sign in', async () => {
    await page.getByLabel(ro.fields.email).fill(email)
    await page.getByLabel(ro.fields.password, { exact: true }).fill(password)
    await page.getByRole('button', { name: ro.login.submit }).click()

    await expect(page.getByRole('heading', { level: 1, name: ro.screens.dashboard })).toBeVisible()
  })

  await test.step('add a vehicle', async () => {
    await page.getByRole('link', { name: ro.navigation.garage, exact: true }).click()
    await page.getByRole('link', { name: ro.garage.add }).click()

    await page.getByLabel(ro.fields.registrationNumber).fill('B 01 E2E')
    await page.getByLabel(ro.fields.make).fill('Dacia')
    await page.getByLabel(ro.fields.commercialDescription).fill('Logan')
    await page.getByLabel(ro.fields.vin).fill('E2E00000JOURNEY01')
    await page.getByRole('button', { name: ro.addVehicle.submit }).click()

    // Creation opens the vehicle it created, named from its certificate.
    await expect(page.getByRole('heading', { level: 1, name: 'Dacia Logan' })).toBeVisible()
  })

  await test.step('open the registration certificate', async () => {
    await page.getByRole('link', { name: ro.certificate.open }).click()

    await expect(
      page.getByRole('heading', { level: 1, name: ro.screens.certificate }),
    ).toBeVisible()
    // The four fields that were required to save the vehicle are on the
    // certificate, because section 9 makes it their only home.
    await expect(page.getByLabel(ro.certificate.fields.vin)).toHaveValue('E2E00000JOURNEY01')

    await page.getByRole('link', { name: ro.certificate.backToVehicle }).click()
  })

  await test.step('add a document', async () => {
    await page.getByRole('link', { name: ro.documents.open }).click()

    await page.getByLabel(ro.documents.fields.type).selectOption('RCA')
    await page.getByLabel(ro.documents.fields.validUntil).fill('2027-12-31')
    await page.getByRole('button', { name: ro.documents.save }).click()

    await expect(page.getByRole('heading', { name: ro.documents.type.RCA })).toBeVisible()
  })
})