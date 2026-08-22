import { fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { ro } from '../i18n/locales/ro.ts'
import { OfflineNotice } from './OfflineNotice.tsx'

/**
 * `navigator.onLine` is a getter on the prototype, so it is spied rather than
 * assigned - and the events are dispatched on window, because that is where the
 * browser fires them and where the hook listens.
 */
let online = true

describe('offline notice', () => {
  beforeEach(() => {
    online = true
    vi.spyOn(navigator, 'onLine', 'get').mockImplementation(() => online)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function connectionLost() {
    online = false
    fireEvent(window, new Event('offline'))
  }

  function connectionBack() {
    online = true
    fireEvent(window, new Event('online'))
  }

  it('says nothing while the browser has a network', () => {
    render(<OfflineNotice />)

    expect(screen.queryByText(ro.common.offline)).not.toBeInTheDocument()
  })

  /**
   * Section 25: the UI must detect network loss rather than fail silently. This
   * is that requirement, asserted at the only place it is visible.
   */
  it('appears when the connection drops', () => {
    render(<OfflineNotice />)

    connectionLost()

    expect(screen.getByText(ro.common.offline)).toBeInTheDocument()
  })

  /**
   * Polite rather than assertive. An alert interrupts whatever a screen reader
   * is saying, which for a condition the person usually caused themselves is
   * rude; status waits its turn.
   */
  it('announces itself politely rather than interrupting', () => {
    render(<OfflineNotice />)

    connectionLost()

    expect(screen.getByRole('status')).toHaveTextContent(ro.common.offline)
  })

  it('goes away when the connection comes back', () => {
    render(<OfflineNotice />)

    connectionLost()
    expect(screen.getByText(ro.common.offline)).toBeInTheDocument()

    connectionBack()

    expect(screen.queryByText(ro.common.offline)).not.toBeInTheDocument()
  })
})