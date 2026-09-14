import { render } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { BrandMark } from './BrandMark.tsx'
import { loadBrandMarks } from './useBrandMark.ts'

describe('BrandMark', () => {
  it('draws the emblem of a make it knows, hidden from assistive technology', async () => {
    const { container } = render(<div><BrandMark make="Dacia" /></div>)
    await loadBrandMarks()

    await expect.poll(() => container.querySelector('svg[data-brand-mark="emblem"]')).not.toBeNull()
    const mark = container.querySelector('svg[data-brand-mark="emblem"]')
    expect(mark).toHaveAttribute('aria-hidden', 'true')
    expect(mark?.querySelector('path')?.getAttribute('d')).toMatch(/^M/)
  })

  it('sets the name in type for a brand it knows without an emblem', async () => {
    const { container } = render(<div><BrandMark make="MERCEDES-BENZ" /></div>)
    await loadBrandMarks()

    await expect.poll(() => container.querySelector('[data-brand-mark="name"]')).not.toBeNull()
    const mark = container.querySelector('[data-brand-mark="name"]')
    expect(mark).toHaveTextContent('Mercedes-Benz')
    expect(mark).toHaveAttribute('aria-hidden', 'true')
    expect(container.querySelector('svg')).toBeNull()
  })

  it('draws nothing at all for a make it does not know', async () => {
    const { container } = render(<div><BrandMark make="Trabant" /></div>)

    await loadBrandMarks()
    await new Promise((resolve) => { setTimeout(resolve, 0) })

    expect(container.querySelector('[data-brand-mark]')).toBeNull()
  })
})