import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import { ro } from '../i18n/locales/ro.ts'
import { paths } from '../routes/paths.ts'
import { renderApp } from '../test/renderApp.tsx'

const PROFILE = {
  id: '1', fullName: 'Marius Robert', email: 'marius@example.com',
  preferredLanguage: 'ro', timezone: 'Europe/Bucharest', emailVerified: true,
}

const LOGAN = {
  id: 'a', registrationNumber: 'B 100 ABC', make: 'Dacia', commercialDescription: 'Logan',
}

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

/** Answers the silent refresh and the profile load, and the garage as told. */
function stubGarage(garage: (attempt: number) => Response) {
  let attempts = 0

  vi.stubGlobal('fetch', vi.fn((input: string) => {
    if (input.includes('/auth/refresh')) {
      return Promise.resolve(jsonResponse(200, {
        accessToken: 'fresh', expiresInSeconds: 600, refreshToken: null,
      }))
    }
    if (!input.includes('/api/v1/vehicles')) {
      return Promise.resolve(jsonResponse(200, PROFILE))
    }
    attempts += 1
    return Promise.resolve(garage(attempts))
  }))
}

describe('garage', () => {
  /**
   * Both labelling rules in one test, because the rule is the choice between
   * them: a nickname wins, and its absence falls back to make and description
   * together rather than to either alone.
   */
  it('lists the vehicles, by nickname where there is one', async () => {
    stubGarage(() => jsonResponse(200, [
      { ...LOGAN, displayName: 'Mașina de teren' },
      { id: 'b', registrationNumber: 'CJ 200 XYZ', make: 'Volkswagen', commercialDescription: 'Golf' },
    ]))

    renderApp(paths.garage)

    expect(await screen.findByRole('link', { name: 'Mașina de teren' })).toBeInTheDocument()
    expect(screen.getByText('B 100 ABC')).toBeInTheDocument()
    expect(screen.getByText('Volkswagen Golf')).toBeInTheDocument()
  })

    it('says the garage is empty', async () => {
    stubGarage(() => jsonResponse(200, []))

    renderApp(paths.garage)

    expect(await screen.findByText(ro.garage.empty)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: ro.garage.add })).toBeInTheDocument()
  })

  it('translates a refused request instead of showing nothing', async () => {
    stubGarage(() => jsonResponse(500, { code: 'INTERNAL_ERROR' }))

    renderApp(paths.garage)

    expect(await screen.findByRole('alert')).toHaveTextContent(ro.errors.INTERNAL_ERROR)
    expect(screen.queryByText(ro.garage.empty)).not.toBeInTheDocument()
  })

  /** Recovery, not merely a second call: the list has to appear afterwards. */
  it('a failed load can be retried', async () => {
    stubGarage(attempt => attempt === 1
      ? jsonResponse(500, { code: 'INTERNAL_ERROR' })
      : jsonResponse(200, [LOGAN]))

    renderApp(paths.garage)
    await screen.findByRole('alert')

    await userEvent.click(screen.getByRole('button', { name: ro.common.retry }))

    expect(await screen.findByText('Dacia Logan')).toBeInTheDocument()
  })
})