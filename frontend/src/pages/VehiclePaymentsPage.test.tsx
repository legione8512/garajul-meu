import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import type { PaymentDetails } from '../api/endpoints/payments.ts'
import { ro } from '../i18n/locales/ro.ts'
import { paths } from '../routes/paths.ts'
import { renderApp } from '../test/renderApp.tsx'

const PROFILE = {
  id: '1', fullName: 'Marius Robert', email: 'marius@example.com',
  preferredLanguage: 'ro', timezone: 'Europe/Bucharest', emailVerified: true,
}

const LOAN: PaymentDetails = {
  id: 'pay-1',
  kind: 'LOAN',
  firstDueDate: '2026-07-15',
  frequency: 'MONTHLY',
  lastDueDate: '2029-06-15',
  instalmentCount: 36,
  remindDaysBefore: [3, 1],
  nextDueDate: '2026-10-15',
  nextInstalment: 4,
  daysUntilNext: 3,
}

interface Sent {
  posts: number
  patches: number
  deletes: number
  body: unknown
  lists: number
}

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function stubPayments(pages: (() => Response)[], write?: () => Response): Sent {
  const sent: Sent = { posts: 0, patches: 0, deletes: 0, body: null, lists: 0 }

  vi.stubGlobal('fetch', vi.fn((input: string, init?: RequestInit) => {
    if (input.includes('/auth/refresh')) {
      return Promise.resolve(jsonResponse(200, {
        accessToken: 'fresh', expiresInSeconds: 600, refreshToken: null,
      }))
    }
    if (input.includes('/payments')) {
      if (init?.method === 'POST' || init?.method === 'PATCH') {
        if (init.method === 'POST') {
          sent.posts += 1
        } else {
          sent.patches += 1
        }
        sent.body = JSON.parse(init.body as string)
        return Promise.resolve(write === undefined ? jsonResponse(201, LOAN) : write())
      }
      if (init?.method === 'DELETE') {
        sent.deletes += 1
        return Promise.resolve(new Response(null, { status: 204 }))
      }
      const answer = pages[Math.min(sent.lists, pages.length - 1)]
      sent.lists += 1
      return Promise.resolve(answer())
    }
    return Promise.resolve(jsonResponse(200, PROFILE))
  }))

  return sent
}

async function open() {
  renderApp(paths.payments('a'))
  await screen.findByRole('heading', { name: ro.payments.title, level: 1 })
}

function addForm() {
  return screen.getByRole('heading', { name: ro.payments.add }).closest('form') as HTMLFormElement
}

describe('vehicle instalments', () => {
  it('says plainly when a vehicle has none, and that no amount is kept', async () => {
    stubPayments([() => jsonResponse(200, [])])

    await open()

    expect(await screen.findByText(ro.payments.none)).toBeInTheDocument()
    expect(screen.getByText(ro.payments.intro)).toBeInTheDocument()
  })

  it('shows a payment with its schedule, its next instalment and its reminders', async () => {
    stubPayments([() => jsonResponse(200, [LOAN])])

    await open()

    const card = (await screen.findByRole('heading', { name: ro.payments.kind.LOAN, level: 2 }))
      .closest('li') as HTMLElement

    expect(card).toHaveTextContent(ro.payments.schedule.many
      .replace('{{frequency}}', ro.payments.frequency.MONTHLY)
      .replace('{{count}}', '36')
      .replace('{{first}}', '15 iulie 2026')
      .replace('{{last}}', '15 iunie 2029'))
    expect(card).toHaveTextContent(ro.payments.next
      .replace('{{date}}', '15 octombrie 2026')
      .replace('{{number}}', '4')
      .replace('{{count}}', '36'))
    expect(within(card).getByText(
      ro.payments.due.few.replace('{{days}}', '3').replace('{{date}}', '15 octombrie 2026'),
    )).toHaveAttribute('data-tone', 'soon')
    expect(card).toHaveTextContent('cu 3 zile înainte, cu o zi înainte')
  })

  it('says so once every instalment has passed', async () => {
    stubPayments([() => jsonResponse(200, [
      { ...LOAN, nextDueDate: null, nextInstalment: null, daysUntilNext: null },
    ])])

    await open()

    expect(await screen.findByText(ro.payments.finished)).toBeInTheDocument()
  })

  it('adds a payment by count with the default reminders, and reloads the list', async () => {
    const sent = stubPayments([() => jsonResponse(200, []), () => jsonResponse(200, [LOAN])])
    const user = userEvent.setup()

    await open()
    const form = addForm()

    await user.type(within(form).getByLabelText(ro.payments.fields.firstDueDate), '2026-10-15')
    await user.type(within(form).getByLabelText(ro.payments.fields.instalmentCount), '36')
    await user.click(within(form).getByRole('button', { name: ro.payments.save }))

    await waitFor(() => { expect(sent.posts).toBe(1) })
    expect(sent.body).toEqual({
      kind: 'LOAN',
      firstDueDate: '2026-10-15',
      frequency: 'MONTHLY',
      lastDueDate: null,
      instalmentCount: 36,
      remindDaysBefore: [3, 1],
    })
    expect(await screen.findByRole('heading', { name: ro.payments.kind.LOAN, level: 2 }))
      .toBeInTheDocument()
  })

  it('sends a last date instead of a count when the person chooses it, with the leads ticked', async () => {
    const sent = stubPayments([() => jsonResponse(200, [])])
    const user = userEvent.setup()

    await open()
    const form = addForm()

    await user.selectOptions(within(form).getByLabelText(ro.payments.fields.kind), 'CASCO')
    await user.selectOptions(within(form).getByLabelText(ro.payments.fields.frequency), 'QUARTERLY')
    await user.selectOptions(within(form).getByLabelText(ro.payments.fields.endBy), 'date')
    await user.type(within(form).getByLabelText(ro.payments.fields.firstDueDate), '2026-10-15')
    await user.type(within(form).getByLabelText(ro.payments.fields.lastDueDate), '2027-07-15')
    await user.click(within(form).getByLabelText(ro.payments.lead.days7))
    await user.click(within(form).getByLabelText(ro.payments.lead.day1))
    await user.click(within(form).getByRole('button', { name: ro.payments.save }))

    await waitFor(() => { expect(sent.posts).toBe(1) })
    expect(sent.body).toEqual({
      kind: 'CASCO',
      firstDueDate: '2026-10-15',
      frequency: 'QUARTERLY',
      lastDueDate: '2027-07-15',
      instalmentCount: null,
      remindDaysBefore: [7, 3],
    })
  })

  it('checks the count before sending anything', async () => {
    const sent = stubPayments([() => jsonResponse(200, [])])
    const user = userEvent.setup()

    await open()
    const form = addForm()

    await user.type(within(form).getByLabelText(ro.payments.fields.firstDueDate), '2026-10-15')
    await user.type(within(form).getByLabelText(ro.payments.fields.instalmentCount), '121')
    await user.click(within(form).getByRole('button', { name: ro.payments.save }))

    expect(await within(form).findByText(ro.validation.instalmentCount)).toBeInTheDocument()
    expect(sent.posts).toBe(0)
  })

  it('shows the backend refusing a schedule in words', async () => {
    stubPayments([() => jsonResponse(200, [])], () => jsonResponse(400, {
      code: 'PAYMENT_INVALID_SCHEDULE', requestId: 'r1',
    }))
    const user = userEvent.setup()

    await open()
    const form = addForm()

    await user.type(within(form).getByLabelText(ro.payments.fields.firstDueDate), '2026-10-15')
    await user.type(within(form).getByLabelText(ro.payments.fields.instalmentCount), '3')
    await user.click(within(form).getByRole('button', { name: ro.payments.save }))

    expect(await within(form).findByText(ro.errors.PAYMENT_INVALID_SCHEDULE)).toBeInTheDocument()
  })

  it('corrects a payment in place, starting from what is stored', async () => {
    const sent = stubPayments([() => jsonResponse(200, [LOAN])])
    const user = userEvent.setup()

    await open()
    await user.click(await screen.findByRole('button', { name: ro.payments.edit }))

    const form = screen.getByRole('button', { name: ro.payments.saveCorrection })
      .closest('form') as HTMLFormElement
    expect(within(form).getByLabelText(ro.payments.fields.lastDueDate)).toHaveValue('2029-06-15')

    await user.click(within(form).getByLabelText(ro.payments.lead.onTheDay))
    await user.click(within(form).getByRole('button', { name: ro.payments.saveCorrection }))

    await waitFor(() => { expect(sent.patches).toBe(1) })
    expect(sent.body).toEqual({
      kind: 'LOAN',
      firstDueDate: '2026-07-15',
      frequency: 'MONTHLY',
      lastDueDate: '2029-06-15',
      instalmentCount: null,
      remindDaysBefore: [3, 1, 0],
    })
  })

  it('asks before it deletes', async () => {
    const sent = stubPayments([() => jsonResponse(200, [LOAN]), () => jsonResponse(200, [])])
    const user = userEvent.setup()

    await open()
    await user.click(await screen.findByRole('button', { name: ro.payments.delete }))

    expect(screen.getByText(ro.payments.confirmDelete)).toBeInTheDocument()
    expect(sent.deletes).toBe(0)

    await user.click(screen.getByRole('button', { name: ro.payments.confirmDeleteYes }))

    await waitFor(() => { expect(sent.deletes).toBe(1) })
    expect(await screen.findByText(ro.payments.none)).toBeInTheDocument()
  })

  it('is reached from the vehicle', async () => {
    vi.stubGlobal('fetch', vi.fn((input: string) => {
      if (input.includes('/auth/refresh')) {
        return Promise.resolve(jsonResponse(200, {
          accessToken: 'fresh', expiresInSeconds: 600, refreshToken: null,
        }))
      }
      if (input.includes('/vehicles/a')) {
        return Promise.resolve(jsonResponse(200, {
          id: 'a', registrationNumber: 'B 100 ABC', make: 'Dacia',
          commercialDescription: 'Logan', vin: 'UU1XXXXXXXX000001', hasImage: false,
          usageType: 'NORMAL', displayName: null,
        }))
      }
      return Promise.resolve(jsonResponse(200, PROFILE))
    }))

    renderApp(paths.vehicle('a'))

    expect(await screen.findByRole('link', { name: ro.payments.open }))
      .toHaveAttribute('href', paths.payments('a'))
  })
})
