import { useId } from 'react'

import { FieldMessage } from './FieldMessage.tsx'
import type { ValidationMessage } from '../forms/rules.ts'

export interface SelectOption {
  readonly value: string
  readonly label: string
}

interface SelectFieldProps {
  readonly label: string
  readonly value: string
  readonly options: readonly SelectOption[]
  readonly onChange: (value: string) => void
  readonly message?: ValidationMessage
}

/**
 * One label, one select, one message, wired together correctly - the same
 * bargain TextField makes and for the same reason. A select needs identical
 * aria-invalid and aria-describedby handling, and writing it inline wherever one
 * is needed would be another chance to get it subtly wrong.
 */
export function SelectField({ label, value, options, onChange, message }: SelectFieldProps) {
  const id = useId()
  const messageId = `${id}-message`
  const invalid = message !== undefined

  return (
    <div>
      <label htmlFor={id}>{label}</label>
      <select
        id={id}
        value={value}
        onChange={(event) => { onChange(event.target.value) }}
        aria-invalid={invalid}
        aria-describedby={invalid ? messageId : undefined}
      >
        {options.map(option => (
          <option key={option.value} value={option.value}>{option.label}</option>
        ))}
      </select>
      <FieldMessage id={messageId} message={message} />
    </div>
  )
}