import { describe, expect, it } from 'vitest'

import { brandCandidates } from './brandKey.ts'

describe('brandCandidates', () => {
  it('reads a make however the certificate or the person wrote it', () => {
    expect(brandCandidates('VOLVO')).toEqual(['VOLVO'])
    expect(brandCandidates('volvo')).toEqual(['VOLVO'])
    expect(brandCandidates('ŠKODA')).toEqual(['SKODA'])
    expect(brandCandidates('Citroën')).toEqual(['CITROEN'])
  })

  it('runs every word together first, then two, then one', () => {
    expect(brandCandidates('DS AUTOMOBILES')).toEqual(['DSAUTOMOBILES', 'DS'])
    expect(brandCandidates('B.M.W.')).toEqual(['BMW', 'BM', 'B'])
    expect(brandCandidates('Ford Werke GmbH')).toEqual(['FORDWERKEGMBH', 'FORDWERKE', 'FORD'])
  })

  /** Words, not prefixes: a make that merely begins like a brand is not that brand. */
  it('never cuts a word short', () => {
    expect(brandCandidates('MINIBUS')).toEqual(['MINIBUS'])
  })

  it('has nothing to offer for a make with no letters or digits in it', () => {
    expect(brandCandidates('')).toEqual([])
    expect(brandCandidates(' - / ')).toEqual([])
  })
})