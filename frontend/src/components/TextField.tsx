import { useId } from 'react'

import type { ValidationMessage } from '../forms/rules.ts'
import { FieldMessage } from './FieldMessage.tsx'

interface TextFieldProps {
  label: string
  value: string
  onChange: (value: string) => void
  message: ValidationMessage | undefined
  type?: string
  autoComplete?: string
  inputMode?: 'text' | 'numeric'
  maxLength?: number
  /** For the one free-text box on a registration certificate. */
  multiline?: boolean
}

/**
 * One label, one input, one message, wired together correctly.
 *
 * <p>The wiring is the whole point of the component existing. Six forms would
 * otherwise repeat aria-invalid and aria-describedby by hand, and
 * eslint-plugin-jsx-a11y cannot be installed to check any of it - so the
 * repetition would be six chances to get it subtly wrong instead of one place
 * to get it right. A textarea needs exactly the same wiring, which is why it
 * lives here rather than being written out wherever one is needed.
 *
 * <p>The id is generated here rather than passed in, so no caller can hand two
 * fields the same one.
 */
export function TextField({
  label,
  value,
  onChange,
  message,
  type = 'text',
  autoComplete,
  inputMode,
  maxLength,
  multiline,
}: TextFieldProps) {
  const id = useId()
  const messageId = `${id}-message`
  const invalid = message !== undefined
  const describedBy = invalid ? messageId : undefined

  return (
    <div>
      <label htmlFor={id}>{label}</label>
      {multiline === true
        ? (
          <textarea
            id={id}
            value={value}
            maxLength={maxLength}
            onChange={(event) => { onChange(event.target.value) }}
            aria-invalid={invalid}
            aria-describedby={describedBy}
          />
          )
        : (
          <input
            id={id}
            type={type}
            value={value}
            autoComplete={autoComplete}
            inputMode={inputMode}
            maxLength={maxLength}
            onChange={(event) => { onChange(event.target.value) }}
            aria-invalid={invalid}
            aria-describedby={describedBy}
          />
          )}
      <FieldMessage id={messageId} message={message} />
    </div>
  )
}