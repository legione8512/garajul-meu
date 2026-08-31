import { useId, useState, type CSSProperties } from 'react'
import { useTranslation } from 'react-i18next'

import type { ScanStatus } from '../api/endpoints/ocr.ts'
import template from '../assets/registration-certificate-template.jpg'
import { certificateFields, type CertificateField, type FieldSpec } from './fields.ts'
import { fieldPositions } from './coordinates.ts'
import type { FieldStatuses } from './scan.ts'
import type { CertificateForm } from './values.ts'
import type { FieldMessages } from '../forms/validate.ts'

interface CertificateOverlayProps {
  form: CertificateForm
  messages: FieldMessages<CertificateField>
  /** Empty until a scan has run, which is most of this screen's life. */
  statuses: FieldStatuses
  onChange: (field: CertificateField, value: string) => void
}

/**
 * The template's own width. Everything is positioned in fractions of it, so this
 * is the one place a pixel appears - and it is a floor rather than a fit.
 *
 * <p>A field box is 0.033 of the template's height, and the template is 1.92
 * times wider than it is tall, so a field is about 1.7% of the displayed width.
 * Twenty usable pixels of field height therefore need roughly 1160 pixels of
 * template. Scaling the whole thing down to a 375-pixel phone would leave each
 * field two pixels tall, which is why section 7 asks for pan and zoom rather
 * than for a responsive shrink.
 */
const TEMPLATE_WIDTH = 1280

const MIN_SCALE = 0.5
const MAX_SCALE = 3
const STEP = 0.25

/**
 * Ink on the template, and <strong>deliberately not `--text`</strong>.
 *
 * <p>Every other colour in this application is a token in `index.css`, because
 * every other colour paints a surface the palette controls. This one paints a
 * photograph of a real document - pale, printed, and no more themeable than a
 * scan of a passport. A local constant says that out loud; a token in `:root`
 * would sit among the theme's colours and invite somebody to make it match them.
 *
 * <p><strong>Found by measurement on 2026-08-25, and it was a release
 * blocker.</strong> This style set `font: inherit` and no colour, which was
 * right while the application was light-themed: the field inherited dark text
 * onto a pale document. Phase 13.5 inverted the palette, the field inherited
 * `--text` at `rgb(232,230,242)`, the template behind it measures
 * `rgb(219,234,230)`, and the contrast was **1.01:1** - the registration number,
 * the make, the model and the VIN were all rendered and none of them was
 * visible. The value below measures about 14:1 against the same template.
 *
 * <p>It also restores the scan outlines, which are drawn in `currentColor` and
 * had gone invisible with the text. One property, both failures.
 */
const DOCUMENT_INK = '#151321'

const percent = (fraction: number) => `${(fraction * 100).toFixed(4)}%`

/**
 * The three states of section 7, told by line style rather than by colour.
 * Colour alone would leave the states invisible to anyone who cannot
 * distinguish them, which WCAG 1.4.1 is precisely about; every field also
 * carries the state in its accessible description, so it is said in words too.
 */
const SCAN_OUTLINE: Record<ScanStatus, string> = {
  DETECTED: '1px solid currentColor',
  NEEDS_REVIEW: '2px dashed currentColor',
  NOT_DETECTED: '1px dotted currentColor',
}

/**
 * A validation problem outranks a scan state: one is something to fix, the other
 * something to know, and a box can only say one thing at a time.
 */
function outlineFor(hasMessage: boolean, status: ScanStatus | undefined): string {
  if (hasMessage) {
    return '2px solid currentColor'
  }
  return status === undefined ? '1px solid transparent' : SCAN_OUTLINE[status]
}

/**
 * Hidden from sight, not from screen readers. The template prints the code next
 * to each box, which is enough for somebody looking at it and nothing at all
 * for somebody who is not - so every field keeps a real label saying what it is.
 */
const HIDDEN: CSSProperties = {
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
 * <p><strong>Panning is ordinary scrolling.</strong> The template sits at a real
 * pixel width inside a container that scrolls, so touch momentum, the mouse
 * wheel, keyboard scrolling and scrolling to a focused field all come from the
 * browser. A hand-written pan built on pointer events and transforms would have
 * to reimplement every one of those, and would get some of them wrong.
 *
 * <p>The scrolling region deliberately has no tabindex. A scrollable area needs
 * one only when nothing inside it can take focus; this one holds thirty-three
 * fields, so the browser already scrolls it as somebody tabs through, and adding
 * a tab stop would only put an extra empty step in the way.
 *
 * <p>Positioning is inline because it is data. The coordinates are fractions in
 * a table, so a stylesheet could only restate them less accurately.
 *
 * <p><strong>Colour is inline for a different reason, and it is worth not
 * confusing the two.</strong> The coordinates are inline because a stylesheet
 * would say them worse; the ink is inline because this component draws on an
 * image rather than on a themed surface, and must not inherit the palette. See
 * {@link DOCUMENT_INK}.
 *
 * <p>The image is decorative - {@code alt=""} - because everything it says is
 * also said by the labelled fields on top of it. Describing it as well would
 * make a screen reader read the whole certificate twice.
 *
 * <p>A field may be laid out more than once. "Numărul certificatului" is printed
 * on two panels and stored once, so both boxes read and write the same value.
 */
export function CertificateOverlay({ form, messages, statuses, onChange }: CertificateOverlayProps) {
  const { t } = useTranslation()
  const baseId = useId()
  const [scale, setScale] = useState(1)

  const step = (by: number) => {
    setScale(previous => Math.min(MAX_SCALE, Math.max(MIN_SCALE, previous + by)))
  }

  return (
    <>
      {/*
        A toolbar, not three primary actions. Painted in the brand colour and
        touching one another, the zoom controls read as the most important thing
        on a screen whose whole point is the document underneath them.
      */}
      <div data-actions>
        <button data-quiet type="button" onClick={() => { step(-STEP) }} disabled={scale <= MIN_SCALE}>
          {t('certificate.zoomOut')}
        </button>
        <button data-quiet type="button" onClick={() => { setScale(1) }} disabled={scale === 1}>
          {t('certificate.zoomReset')}
        </button>
        <button data-quiet type="button" onClick={() => { step(STEP) }} disabled={scale >= MAX_SCALE}>
          {t('certificate.zoomIn')}
        </button>
        <span role="status">{t('certificate.zoomLevel', { percent: Math.round(scale * 100) })}</span>
      </div>

      <div style={{ overflow: 'auto', maxWidth: '100%' }}>
        <div style={{ position: 'relative', width: TEMPLATE_WIDTH * scale }}>
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
            const status = statuses[position.name]
            const value = form[position.name]

            // One description slot. After a scan every coded field has a state,
            // which is deliberately verbose: it is the only way somebody not
            // looking at the picture learns which values came from it.
            const description = message !== undefined
              ? t(message.key, message.values)
              : status === undefined
                ? undefined
                : t(`certificate.scan.status.${status}`)

            const box: CSSProperties = {
              position: 'absolute',
              left: percent(position.x),
              top: percent(position.y),
              width: percent(position.w),
              height: percent(position.h),
            }

            const control: CSSProperties = {
              width: '100%',
              height: '100%',
              boxSizing: 'border-box',
              background: 'transparent',
              // Both the typed value and, through currentColor, the scan outline.
              color: DOCUMENT_INK,
              border: outlineFor(message !== undefined, status),
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
                      aria-describedby={description === undefined ? undefined : describedBy}
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
                        aria-describedby={description === undefined ? undefined : describedBy}
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
                        aria-describedby={description === undefined ? undefined : describedBy}
                      />
                      )}

                {description !== undefined && (
                  <span id={describedBy} style={HIDDEN}>{description}</span>
                )}
              </div>
            )
          })}
        </div>
      </div>
    </>
  )
}