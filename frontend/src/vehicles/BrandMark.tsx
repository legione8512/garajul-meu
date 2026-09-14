import { useBrandMark } from './useBrandMark.ts'

/**
 * The emblem of a vehicle's make on the right of its card, or the brand's name
 * set in type when there is no emblem for it, or nothing for a make nobody
 * recognises.
 *
 * <p>Hidden from assistive technology, because it says nothing the card has not
 * already said in words: the vehicle's name is its make and description, or a
 * nickname the person chose. Reading "Volvo" a second time before it would be
 * noise, and a nickname is the person's choice not to be told the make.
 *
 * <p>Drawn in `fill: currentColor` so the stylesheet decides its colour; the
 * paths are Simple Icons' 24-unit squares.
 */
export function BrandMark({ make }: { make: string }) {
  const mark = useBrandMark(make)

  if (mark === null) {
    return null
  }

  if (mark.kind === 'name') {
    return <span data-brand-mark="name" aria-hidden="true">{mark.name}</span>
  }

  return (
    <svg data-brand-mark="emblem" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path d={mark.path} />
    </svg>
  )
}