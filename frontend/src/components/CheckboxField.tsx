import { useId } from 'react'

interface CheckboxFieldProps {
  label: string
  checked: boolean
  onChange: (checked: boolean) => void
  /**
   * Exposes the box as a switch - `role='switch'`, drawn as a track and knob by
   * index.css - for a control that turns one thing on and off. Screen readers
   * then say "on" and "off" rather than "checked". A choice among several
   * options stays a checkbox.
   */
  asSwitch?: boolean
}

/**
 * One checkbox with its label attached, for the same reason TextField exists:
 * the association is the part no linter here is checking, since
 * eslint-plugin-jsx-a11y still cannot be installed.
 *
 * <p>The label follows the box rather than preceding it - that is the reading
 * order for a checkbox, and it is how the certificate itself prints C2=C1.
 *
 * <p>No message slot. The two checkboxes on a certificate are optional booleans
 * with nothing to get wrong, and an unused affordance invites somebody to find a
 * use for it.
 */
export function CheckboxField({ label, checked, onChange, asSwitch = false }: CheckboxFieldProps) {
  const id = useId()

  return (
    <div>
      <input
        id={id}
        type="checkbox"
        role={asSwitch ? 'switch' : undefined}
        checked={checked}
        onChange={(event) => { onChange(event.target.checked) }}
      />
      <label htmlFor={id}>{label}</label>
    </div>
  )
}