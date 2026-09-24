import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import { ro } from '../i18n/locales/ro.ts'
import { paths } from '../routes/paths.ts'
import { renderApp } from '../test/renderApp.tsx'

const PROFILE = {
  id: '1', fullName: 'Ana Pop', email: 'ana@example.com',
  preferredLanguage: 'ro', timezone: 'Europe/Bucharest', emailVerified: true,
}

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

interface Sent {
  bodies: unknown[]
}

function stubFeedback(answer: () => Response = () => new Response(null, { status: 204 })): Sent {
  const sent: Sent = { bodies: [] }

  vi.stubGlobal('fetch', vi.fn((input: string, init?: RequestInit) => {
    if (input.includes('/auth/refresh')) {
      return Promise.resolve(jsonResponse(200, {
        accessToken: 'fresh', expiresInSeconds: 600, refreshToken: null,
      }))
    }
    if (input.includes('/feedback')) {
      sent.bodies.push(JSON.parse(init?.body as string))
      return Promise.resolve(answer())
    }
    return Promise.resolve(jsonResponse(200, PROFILE))
  }))

  return sent
}

async function open() {
  renderApp(paths.feedback)
  await screen.findByRole('heading', { name: ro.screens.feedback, level: 1 })
}

describe('the Sugestii tab', () => {
  it('says what it is for and where the answer comes', async () => {
    stubFeedback()

    await open()

    expect(screen.getByText(ro.feedback.intro)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: ro.navigation.feedback }))
      .toHaveAttribute('aria-current', 'page')
  })

  it('sends the kind, the message and the platform, then thanks and empties the form', async () => {
    const sent = stubFeedback()
    const user = userEvent.setup()

    await open()

    await user.selectOptions(screen.getByLabelText(ro.feedback.category), 'PROBLEM')
    await user.type(screen.getByLabelText(ro.feedback.message), '  Nu primesc notificări  ')
    await user.click(screen.getByRole('button', { name: ro.feedback.send }))

    expect(await screen.findByRole('status')).toHaveTextContent(ro.feedback.thanks)
    expect(sent.bodies).toEqual([{
      category: 'PROBLEM',
      message: 'Nu primesc notificări',
      platform: 'WEB',
      appVersion: null,
    }])
    expect(screen.getByLabelText(ro.feedback.message)).toHaveValue('')
    expect(screen.getByLabelText(ro.feedback.category)).toHaveValue('IDEA')
  })

  it('sends nothing without a message', async () => {
    const sent = stubFeedback()
    const user = userEvent.setup()

    await open()
    await user.click(screen.getByRole('button', { name: ro.feedback.send }))

    expect(await screen.findByText(ro.validation.required)).toBeInTheDocument()
    expect(sent.bodies).toHaveLength(0)
  })

  it('says in words when the day\'s five are spent', async () => {
    stubFeedback(() => jsonResponse(429, { code: 'FEEDBACK_LIMIT_REACHED', requestId: 'r1' }))
    const user = userEvent.setup()

    await open()
    await user.type(screen.getByLabelText(ro.feedback.message), 'Încă o idee')
    await user.click(screen.getByRole('button', { name: ro.feedback.send }))

    expect(await screen.findByText(ro.errors.FEEDBACK_LIMIT_REACHED)).toBeInTheDocument()
    await waitFor(() => { expect(screen.queryByRole('status')).toBeNull() })
    expect(screen.getByLabelText(ro.feedback.message)).toHaveValue('Încă o idee')
  })
})
