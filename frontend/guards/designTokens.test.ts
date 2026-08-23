import { readFileSync } from 'node:fs'

import { expect, test } from 'vitest'

/**
 * The stylesheet and the tone vocabulary are two files that have to agree, and
 * nothing renders them together: `status.ts` invents the names, `index.css`
 * styles them, and a tone added to one and forgotten in the other produces an
 * unstyled line rather than an error.
 *
 * Node-side, so it lives in `guards/` beside `productionEnv.test.ts` - `src` is
 * browser-typed and cannot read a file from disk.
 */

const css = readFileSync('src/index.css', 'utf8')
const status = readFileSync('src/documents/status.ts', 'utf8')

/** The union, read from the declaration rather than copied beside it. */
function declaredTones(): string[] {
  const declaration = status.match(/export type DocumentTone\s*=([^\n]+)/)
  if (declaration === null) {
    throw new Error('DocumentTone is no longer declared on one line in status.ts')
  }

  return [...declaration[1].matchAll(/'([a-z]+)'/g)].map((match) => match[1])
}

/** The `border-left` shorthand of one tone, minus its colour. */
function borderOf(tone: string): string {
  const rule = css.match(new RegExp(`\\[data-tone='${tone}'\\]\\s*\\{([^}]*)\\}`))
  if (rule === null) {
    throw new Error(`index.css has no rule for [data-tone='${tone}']`)
  }

  const border = rule[1].match(/border-left:\s*([^;]+);/)
  if (border === null) {
    throw new Error(`[data-tone='${tone}'] sets no border-left, so colour is its only signal`)
  }

  // "3px solid var(--tone-ok)" -> "3px solid". The colour is deliberately
  // dropped: this test is about what remains when colour is unavailable.
  return border[1].trim().split(/\s+/).slice(0, 2).join(' ')
}

test('every declared tone is styled', () => {
  const missing = declaredTones().filter((tone) => !css.includes(`[data-tone='${tone}']`))

  expect(missing, 'tones in status.ts with no rule in index.css').toEqual([])
})

/**
 * The constraint the whole palette was allowed to exist under: colour may be
 * added, but never as the only difference. Two tones sharing a border are
 * indistinguishable on a monochrome screen, on a printout, and to roughly one
 * man in twelve.
 */
test('no two tones are told apart by colour alone', () => {
  const tones = declaredTones()
  const borders = tones.map(borderOf)

  expect(new Set(borders).size, `borders were ${borders.join(', ')}`).toBe(tones.length)
})