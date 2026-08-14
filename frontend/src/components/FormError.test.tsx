import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { ApiError } from '../api/ApiError.ts'
import { ro } from '../i18n/locales/ro.ts'
import { FormError } from './FormError.tsx'

describe('form error', () => {
  it('renders nothing when there is no error', () => {
    const { container } = render(<FormError error={null} />)

    expect(container).toBeEmptyDOMElement()
  })

  it('translates a backend code into something a person can read', () => {
    render(<FormError error={new ApiError('INVALID_CREDENTIALS', 401, null, [])} />)

    expect(screen.getByRole('alert')).toHaveTextContent(ro.errors.INVALID_CREDENTIALS)
  })

  /** Nothing to act on, so the one useful thing is the number to quote. */
  it('offers the reference when the failure is opaque', () => {
    render(<FormError error={new ApiError('INTERNAL_ERROR', 500, 'abc-123', [])} />)

    expect(screen.getByRole('alert')).toHaveTextContent('abc-123')
  })
})