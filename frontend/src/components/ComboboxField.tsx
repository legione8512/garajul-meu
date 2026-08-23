import { useId } from 'react'

import { FieldMessage } from './FieldMessage.tsx'
import type { SelectOption } from './SelectField.tsx'
import type { ValidationMessage } from '../forms/rules.ts'

interface ComboboxFieldProps {
  readonly label: string
  readonly value: string
  readonly options: readonly SelectOption[]
  readonly onChange: (value: string) => void
  readonly message?: ValidationMessage
}

/**
 * A text field with suggestions, for lists too long to choose from by scrolling.
 *
 * <p><strong>This exists because of a measurement, not a preference.</strong> The
 * time-zone field was a `select` carrying every IANA zone - several hundred,
 * beginning at Africa/Abidjan. On 2026-08-22 the profile screen's accessibility
 * tree came to 30,638 characters, almost all of it options, and truncated three
 * separate attempts to read the page. On a phone the control is a wheel somebody
 * has to spin past two hundred entries to reach Europe/Bucharest.
 *
 * <p>`input` plus `datalist` rather than a hand-built typeahead: the browser
 * supplies the filtering, the keyboard handling and the screen-reader semantics,
 * and on a phone it renders as the platform's own filtered list. A custom one
 * would be several hundred lines with its own focus bugs.
 *
 * <p>The cost is that free text can be typed, so <strong>the caller must
 * validate</strong> - a datalist suggests, it does not constrain. That is not a
 * gap being tolerated: the backend validates the zone against
 * `ZoneId.getAvailableZoneIds()` regardless, so a client-side check is about
 * answering immediately rather than about safety.
 *
 * <p>For lists whose value differs from what is shown, use `SelectField`. A
 * datalist matches on the value, so a code shown as a name would have to be
 * typed as the code.
 */
export function ComboboxField({ label, value, options, onChange, message }: ComboboxFieldProps) {
  const id = useId()
  const listId = `${id}-list`
  const messageId = `${id}-message`
  const invalid = message !== undefined

  return (
    <div>
      <label htmlFor={id}>{label}</label>
      <input
        id={id}
        list={listId}
        value={value}
        onChange={(event) => { onChange(event.target.value) }}
        aria-invalid={invalid}
        aria-describedby={invalid ? messageId : undefined}
        // The browser's own history would offer values from other sites'
        // fields, which for a zone list is noise on top of the real suggestions.
        autoComplete="off"
        spellCheck={false}
      />
      <datalist id={listId}>
        {options.map(option => (
          <option key={option.value} value={option.value} />
        ))}
      </datalist>
      <FieldMessage id={messageId} message={message} />
    </div>
  )
}