import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import { ComboboxField } from './ComboboxField.tsx'

const zones = [
  { value: 'Europe/Bucharest', label: 'Europe/Bucharest' },
  { value: 'Europe/London', label: 'Europe/London' },
]

describe('ComboboxField', () => {
    /**
   * The suggestions must be reachable from the input, which is the whole point
   * of the control: `list` and the datalist's `id` are what the browser joins,
   * and a mismatch produces a plain text box that silently offers nothing.
   *
   * <p>Queried through the DOM rather than by role, deliberately. A datalist's
   * options carry no `option` role - that role belongs to a listbox - and they
   * have no accessible name, because the value is an attribute and not text.
   * What matters here is the join and the payload, and both are attributes.
   */
  it('joins the input to its suggestion list', () => {
    render(<ComboboxField label="Fus orar" value="" options={zones} onChange={vi.fn()} />)

    const input = screen.getByLabelText('Fus orar')
    const list = document.getElementById(input.getAttribute('list') ?? '')

    expect(list?.tagName).toBe('DATALIST')
    expect([...(list?.querySelectorAll('option') ?? [])].map(option => option.value))
      .toEqual(['Europe/Bucharest', 'Europe/London'])
  })

  it('reports what was typed, character by character', async () => {
    const onChange = vi.fn()
    render(<ComboboxField label="Fus orar" value="" options={zones} onChange={onChange} />)

    await userEvent.type(screen.getByLabelText('Fus orar'), 'Eu')

    expect(onChange).toHaveBeenCalledTimes(2)
    expect(onChange).toHaveBeenLastCalledWith('u')
  })

  /**
   * A message must reach the field itself, not only the eye. `aria-invalid` is
   * what a screen reader announces and `aria-describedby` is what points it at
   * the explanation - the same bargain TextField and SelectField make.
   */
  it('announces an invalid value rather than only colouring it', () => {
    render(
      <ComboboxField
        label="Fus orar"
        value="Europe/Bucuresti"
        options={zones}
        onChange={vi.fn()}
        message={{ key: 'validation.invalid' }}
      />,
    )

    const input = screen.getByLabelText('Fus orar')

    expect(input).toHaveAttribute('aria-invalid', 'true')
    expect(input).toHaveAccessibleDescription()
  })
})