import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import { CheckboxField } from './CheckboxField.tsx'

describe('CheckboxField', () => {
  /** The whole reason the component exists, and the part no linter checks here. */
  it('the label reaches the box', () => {
    render(<CheckboxField label="C2=C1" checked={false} onChange={() => undefined} />)

    expect(screen.getByLabelText('C2=C1')).toHaveAttribute('type', 'checkbox')
  })

  it('shows the state it is given', () => {
    render(<CheckboxField label="C2=C1" checked onChange={() => undefined} />)

    expect(screen.getByLabelText('C2=C1')).toBeChecked()
  })

  it('reports what it was changed to, not that it changed', () => {
    const onChange = vi.fn()
    render(<CheckboxField label="C2=C1" checked={false} onChange={onChange} />)

    return userEvent.click(screen.getByLabelText('C2=C1')).then(() => {
      expect(onChange).toHaveBeenCalledWith(true)
    })
  })
})