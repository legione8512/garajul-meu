import { readFileSync } from 'node:fs'

import { describe, expect, it } from 'vitest'

/**
 * The certificate's fields are shaped by index.css in two opposite directions:
 * the form rule makes every input a comfortable form field, and a rule scoped to
 * `[data-certificate]` takes that back for the fields laid over the document.
 * Nothing renders the two together in a unit test - jsdom applies no stylesheet
 * - and on 2026-09-14 the absence of the second rule had doubled every field on
 * the template, in every browser, for three weeks without a test noticing.
 *
 * So this reads the stylesheet. It is a guard on text rather than on layout,
 * and the layout was measured in Chrome and on the iPad simulator the day the
 * rule was written: 33 fields, each exactly the size of its box.
 */

const css = readFileSync('src/index.css', 'utf8')

/** The declarations of the rule whose selector list is exactly `selector`. */
function declarationsOf(selector: string): Map<string, string> {
  const escaped = selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&').replace(/\s+/g, '\\s*')
  // Preceded by the end of a rule or of a comment, so that a longer selector
  // list merely ending in the same words is not mistaken for this one.
  const rule = css.match(new RegExp(`(?:^|\\}|\\*/)\\s*${escaped}\\s*\\{([^}]*)\\}`))

  if (rule === null) {
    throw new Error(`index.css has no rule for ${selector}`)
  }

  return new Map(
    rule[1]
      .replace(/\/\*[\s\S]*?\*\//g, '')
      .split(';')
      // At the first colon only: a value such as a data URL has colons of its own.
      .map((part): [string, string] => {
        const colon = part.indexOf(':')
        return colon < 0 ? ['', ''] : [part.slice(0, colon).trim(), part.slice(colon + 1).trim()]
      })
      .filter(([property]) => property !== ''),
  )
}

describe('the certificate stylesheet', () => {
  it('still has the form rule it exists to answer', () => {
    const form = declarationsOf(`input:not([type='checkbox'], [type='radio']),
select,
textarea`)

    expect(form.get('min-height')).toBe('var(--touch)')
  })

  it('takes the form shape back for every field on the template', () => {
    const fields = declarationsOf(`[data-certificate] input:not([type='checkbox'], [type='radio']),
[data-certificate] textarea`)

    expect(fields.get('min-height')).toBe('0')
    expect(fields.get('margin')).toBe('0')
    expect(fields.get('border-radius')).toBe('0')
    expect(fields.get('padding')).toMatch(/^0\b/)
  })

  /**
   * The template prints a tick in the C2=C1 square. A certificate checkbox left
   * with its native appearance - transparent where it is not drawn - shows that
   * tick whatever its state, which is how an unticked box read as ticked.
   */
  it('paints the certificate checkbox over the printed one', () => {
    const box = declarationsOf(`[data-certificate] input[type='checkbox']`)

    expect(box.get('appearance')).toBe('none')
    expect(box.get('background')).toMatch(/^#[0-9a-f]{6}\b/i)

    const ticked = declarationsOf(`[data-certificate] input[type='checkbox']:checked`)
    expect(ticked.get('background-image')).toContain('path')
  })
})

/**
 * The same kind of defect outside the certificate, and the reason the rule that
 * fixed it there moved to every form: iOS gives a date or time input with its
 * native appearance a width of its own, which ran 23 pixels past the
 * add-document form on 2026-09-17. Measured on the iOS 27 simulator and in
 * Chrome that day; jsdom has neither engine, so this reads the rule.
 */
describe('the date and time inputs', () => {
  it('are drawn as text in a box in every form, not as native controls', () => {
    const inputs = declarationsOf(`input[type='date'],
input[type='time']`)

    expect(inputs.get('appearance')).toBe('none')
    expect(inputs.get('-webkit-appearance')).toBe('none')
    expect(inputs.get('min-width')).toBe('0')
  })

  it('start their value on the left, as every other field does', () => {
    const value = declarationsOf('input::-webkit-date-and-time-value')

    expect(value.get('text-align')).toBe('start')
  })
})