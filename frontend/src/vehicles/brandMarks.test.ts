import { describe, expect, it } from 'vitest'

import { markFor } from './brandMarks.ts'

/** The emblem's title, or undefined when the make got a name or nothing. */
function emblemOf(make: string): string | undefined {
  const mark = markFor(make)
  return mark?.kind === 'emblem' ? mark.title : undefined
}

/**
 * Against the real Simple Icons data rather than a stub: the table is a list of
 * imports, and what needs holding is that each spelling reaches a real path.
 */
describe('markFor', () => {
  it('finds the emblem for the makes a certificate writes', () => {
    expect(emblemOf('DACIA')).toBe('Dacia')
    expect(emblemOf('VOLVO')).toBe('Volvo')
    expect(emblemOf('Škoda')).toBe('ŠKODA')
    expect(emblemOf('VOLKSWAGEN')).toBe('Volkswagen')
    expect(emblemOf('VW')).toBe('Volkswagen')
    expect(emblemOf('DS AUTOMOBILES')).toBe('DS Automobiles')
    expect(emblemOf('KIA MOTORS')).toBe('Kia')
  })

  it('answers with a drawable path', () => {
    const renault = markFor('Renault')
    expect(renault?.kind === 'emblem' ? renault.path : '').toMatch(/^M/)
  })

  /**
   * Recognised without an emblem: the name in type instead (2026-09-14). The
   * certificate writes Mercedes-Benz both ways, with and without the Benz.
   */
  it('names a brand it recognises but has no emblem for', () => {
    expect(markFor('MERCEDES-BENZ')).toEqual({ kind: 'name', name: 'Mercedes-Benz' })
    expect(markFor('MERCEDES BENZ AG')).toEqual({ kind: 'name', name: 'Mercedes-Benz' })
    expect(markFor('Land Rover')).toEqual({ kind: 'name', name: 'Land Rover' })
    expect(markFor('ALFA ROMEO')).toEqual({ kind: 'name', name: 'Alfa Romeo' })
  })

  /** The developer's rule: an unrecognised make shows nothing. */
  it('answers null for a make it does not know, rather than a guess', () => {
    expect(markFor('TRABANT')).toBeNull()
    expect(markFor('ROVER')).toBeNull()
    expect(markFor('MINIBUS')).toBeNull()
    expect(markFor('')).toBeNull()
  })
})