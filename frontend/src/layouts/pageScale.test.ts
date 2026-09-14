import { afterEach, describe, expect, it } from 'vitest'

import { lockPageScale } from './pageScale.ts'

/** The viewport index.html ships, spelled out so a change there is a decision here too. */
const SHIPPED = 'width=device-width, initial-scale=1.0, viewport-fit=cover'

function viewport(content: string): HTMLMetaElement {
  const meta = document.createElement('meta')
  meta.name = 'viewport'
  meta.content = content
  document.head.appendChild(meta)
  return meta
}

afterEach(() => {
  document.head.querySelectorAll('meta[name="viewport"]').forEach((meta) => { meta.remove() })
})

describe('lockPageScale', () => {
  it('holds the scale at 1 and keeps what the viewport already said', () => {
    const meta = viewport(SHIPPED)

    lockPageScale(document)

    const parts = meta.content.split(', ')
    expect(parts).toEqual(expect.arrayContaining([
      'width=device-width', 'initial-scale=1.0', 'viewport-fit=cover',
      'minimum-scale=1', 'maximum-scale=1', 'user-scalable=no',
    ]))
    expect(parts).toHaveLength(6)
  })

  /**
   * `viewport-fit=cover` is what makes the safe-area insets real numbers. A lock
   * that rewrote the viewport from scratch would put the header back under the
   * clock on every phone with a notch.
   */
  it('never drops viewport-fit', () => {
    const meta = viewport(SHIPPED)

    lockPageScale(document)

    expect(meta.content).toContain('viewport-fit=cover')
  })

  it('replaces a scale limit it finds rather than stating a second one', () => {
    const meta = viewport(`${SHIPPED}, maximum-scale=5, user-scalable=yes`)

    lockPageScale(document)
    lockPageScale(document)

    expect(meta.content.match(/maximum-scale/g)).toHaveLength(1)
    expect(meta.content.match(/user-scalable/g)).toHaveLength(1)
    expect(meta.content).toContain('maximum-scale=1')
    expect(meta.content).toContain('user-scalable=no')
  })

  it('leaves a document without a viewport alone', () => {
    expect(() => { lockPageScale(document) }).not.toThrow()
    expect(document.head.querySelector('meta[name="viewport"]')).toBeNull()
  })
})