import { useId } from 'react'
import { useTranslation } from 'react-i18next'

import template from '../assets/registration-certificate-template.jpg'
import { certificateFields, type CertificateField, type FieldSpec } from './fields.ts'
import { fieldPositions } from './coordinates.ts'
import type { CertificateForm } from './values.ts'
import type { FieldMessages } from '../forms/validate.ts'

interface CertificateOverlayProps {
  form: CertificateForm
  messages: FieldMessages<CertificateField>
  onChange: (field: CertificateField, value: string) => void
}

const percent = (fraction: number) => `${(fraction * 100).toFixed(4)}%`

/**
 * Hidden from sight, not from screen readers. The template prints the code next
 * to each box, which is enough for somebody looking at it and nothing at all
 * for somebody who is not - so every field keeps a real label saying what it is.
 */
const HIDDEN: React.CSSProperties = {
  position: 'absolute',
  width: 1,
  height: 1,
  padding: 0,
  margin: -1,
  overflow: 'hidden',
  clipPath: 'inset(50%)',
  whiteSpace: 'nowrap',
  border: 0,
}

function specOf(name: CertificateField): FieldSpec | undefined {
  return certificateFields.find(field => field.name === name)
}

/**
 * The registration certificate as section 7 describes it: one approved template
 * image, with editable fields laid over it at the coordinates measured in
 * `coordinates.ts`. No FRONT/BACK faces, no redesign into panels or cards.
 *
 * <p>Positioning is inline because it is data. The coordinates are fractions in
 * a table, so a stylesheet could only restate them less accurately, and this
 * project has no design system yet to put them in.
 *
 * <p>The image is decorative - {@code alt=""} - because everything it says is
 * also said by the labelled fields on top of it. Describing it as well would
 * make a screen reader read the whole certificate twice.
 *
 * <p>A field may be laid out more than once. "Numărul certificatului" is printed
 * on two panels and stored once, so both boxes read and write the same value.
 */
export function CertificateOverlay({ form, messages, onChange }: CertificateOverlayProps) {
  const { t } = useTranslation()
  const baseId = useId()

  return (
    <div style={{ position: 'relative', width: '100%', maxWidth: 1280, margin: '0 auto' }}>
      <img src={template} alt="" style={{ display: 'block', width: '100%' }} />

      {fieldPositions.map((position, index) => {
        const spec = specOf(position.name)
        if (spec === undefined) {
          return null
        }

        const id = `${baseId}-${String(index)}`
        const describedBy = `${id}-message`
        const label = t(`certificate.fields.${position.name}`)
        const message = messages[position.name]
        const value = form[position.name]

        const box: React.CSSProperties = {
          position: 'absolute',
          left: percent(position.x),
          top: percent(position.y),
          width: percent(position.w),
          height: percent(position.h),
        }

        const control: React.CSSProperties = {
          width: '100%',
          height: '100%',
          boxSizing: 'border-box',
          background: 'transparent',
          border: message === undefined ? '1px solid transparent' : '2px solid currentColor',
          font: 'inherit',
        }

        return (
          <div key={id} style={box}>
            <label htmlFor={id} style={HIDDEN}>{label}</label>

            {spec.kind === 'boolean'
              ? (
                <input
                  id={id}
                  type="checkbox"
                  checked={value === 'true'}
                  onChange={(event) => { onChange(position.name, event.target.checked ? 'true' : '') }}
                  style={{ width: '100%', height: '100%' }}
                  aria-invalid={message !== undefined}
                  aria-describedby={message === undefined ? undefined : describedBy}
                />
                )
              : spec.kind === 'multiline'
                ? (
                  <textarea
                    id={id}
                    value={value}
                    maxLength={spec.maxLength}
                    onChange={(event) => { onChange(position.name, event.target.value) }}
                    style={{ ...control, resize: 'none' }}
                    aria-invalid={message !== undefined}
                    aria-describedby={message === undefined ? undefined : describedBy}
                  />
                  )
                : (
                  <input
                    id={id}
                    type={spec.kind === 'date' ? 'date' : 'text'}
                    // Numbers are text inputs on purpose: a number input in a
                    // Romanian browser refuses "66,5".
                    inputMode={spec.kind === 'integer' || spec.kind === 'decimal' ? 'numeric' : undefined}
                    maxLength={spec.maxLength}
                    value={value}
                    onChange={(event) => { onChange(position.name, event.target.value) }}
                    style={control}
                    aria-invalid={message !== undefined}
                    aria-describedby={message === undefined ? undefined : describedBy}
                  />
                  )}

            {message !== undefined && (
              <span id={describedBy} style={HIDDEN}>{t(message.key, message.values)}</span>
            )}
          </div>
        )
      })}
    </div>
  )
}