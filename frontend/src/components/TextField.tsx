import { useId, useState } from 'react'
import { useTranslation } from 'react-i18next'

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
 *
 * <p><strong>A password field reveals itself, and no caller asks for it.</strong>
 * Every password in the application goes through here, so the choice is made
 * once rather than remembered four times - and a screen that forgot would be the
 * one where somebody mistypes. The button is a real toggle: it carries
 * `aria-pressed` for anything reading the state, and its own label changes for
 * anybody reading the words, because one without the other leaves half the
 * audience guessing.
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
  const { t } = useTranslation()
  const id = useId()
  const messageId = `${id}-message`
  const invalid = message !== undefined
  const describedBy = invalid ? messageId : undefined

  const isPassword = type === 'password'
  const [revealed, setRevealed] = useState(false)

  const control = multiline === true
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
        // The field stays a password field in every other respect - the
        // autocomplete hint, the manager that fills it - and only what is drawn
        // on the glass changes.
        type={isPassword && revealed ? 'text' : type}
        value={value}
        autoComplete={autoComplete}
        inputMode={inputMode}
        maxLength={maxLength}
        onChange={(event) => { onChange(event.target.value) }}
        aria-invalid={invalid}
        aria-describedby={describedBy}
      />
      )

  return (
    <div>
      <label htmlFor={id}>{label}</label>

      {isPassword
        ? (
          <div data-field-row>
            {control}
            <button
              data-quiet
              type="button"
              aria-pressed={revealed}
              onClick={() => { setRevealed(shown => !shown) }}
            >
              {revealed ? t('fields.hidePassword') : t('fields.showPassword')}
            </button>
          </div>
          )
        : control}

      <FieldMessage id={messageId} message={message} />
    </div>
  )
}