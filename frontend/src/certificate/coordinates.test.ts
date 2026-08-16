import { describe, expect, it } from 'vitest'

import { certificateFields } from './fields.ts'
import { fieldPositions } from './coordinates.ts'

describe('certificate coordinates', () => {
  /**
   * A field with no position would simply never appear on the template, and
   * nothing else would say so - the screen would render one box fewer than the
   * document has, which is exactly the kind of absence nobody notices.
   */
  it('every field has somewhere to be', () => {
    const placed = new Set(fieldPositions.map(position => position.name))

    for (const field of certificateFields) {
      expect(placed.has(field.name), field.name).toBe(true)
    }
  })

  /** The reverse: a position for a field that no longer exists. */
  it('every position belongs to a field', () => {
    const known = new Set(certificateFields.map(field => field.name))

    for (const position of fieldPositions) {
      expect(known.has(position.name), position.name).toBe(true)
    }
  })

  /**
   * Fractions of the template, never pixels - that is what lets a sharper scan
   * replace the image without recalibrating anything.
   */
  it('every box lies inside the template', () => {
    for (const position of fieldPositions) {
      expect(position.x, position.name).toBeGreaterThanOrEqual(0)
      expect(position.y, position.name).toBeGreaterThanOrEqual(0)
      expect(position.x + position.w, position.name).toBeLessThanOrEqual(1)
      expect(position.y + position.h, position.name).toBeLessThanOrEqual(1)
    }
  })
})