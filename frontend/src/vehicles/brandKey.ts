/**
 * The ways a make, as written on a certificate or typed by a person, can name a
 * brand - most specific first.
 *
 * <p>The make is D.1, whatever the scan read or the person wrote there, so the
 * same brand arrives as "VOLKSWAGEN", "Volkswagen AG", "ŠKODA", "Skoda" or
 * "B.M.W.". Diacritics go, case goes, and everything that is not a letter or a
 * digit separates words. Then three guesses: every word run together, which is
 * how "DS AUTOMOBILES" and "B.M.W." are recognised; the first two words, for a
 * brand of two words followed by a company suffix; and the first word, which is
 * how "KIA MOTORS" and "FORD WERKE GMBH" are.
 *
 * <p>Words, never prefixes of words. "MINIBUS" does not guess "MINI", and a make
 * that is not in the table simply finds nothing - which is the whole of what an
 * unrecognised make is allowed to do.
 */
export function brandCandidates(make: string): string[] {
  const words = make
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .toUpperCase()
    .split(/[^A-Z0-9]+/)
    .filter(word => word !== '')

  if (words.length === 0) {
    return []
  }

  return [...new Set([words.join(''), words.slice(0, 2).join(''), words[0]])]
}