import { useEffect, useState } from 'react'

import type { BrandMark } from './brandMarks.ts'

type Marks = typeof import('./brandMarks.ts')

/**
 * The emblem table, fetched once for the whole application and only when a card
 * first asks for it. See brandMarks.ts for why it is not in the main bundle.
 */
let marks: Promise<Marks> | null = null

export function loadBrandMarks(): Promise<Marks> {
  marks ??= import('./brandMarks.ts')
  return marks
}

/**
 * The emblem for a make, or `null` - both while the table is still loading and
 * for good when the make is not recognised, so a card never shows anything it
 * would later have to take back.
 *
 * <p>A failed load is the same `null` and deliberately says nothing: the emblem
 * is decoration, and a vehicle card without one is exactly what an unknown make
 * already looks like.
 */
export function useBrandMark(make: string): BrandMark | null {
  const [mark, setMark] = useState<BrandMark | null>(null)

  useEffect(() => {
    let current = true

    loadBrandMarks().then(
      (table) => {
        if (current) {
          setMark(table.markFor(make))
        }
      },
      () => {
        // Decoration: no emblem is the honest fallback. See above.
      },
    )

    return () => {
      current = false
    }
  }, [make])

  return mark
}