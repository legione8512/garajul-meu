/**
 * The pictures beside the primary navigation's three words, since 1.0.2
 * (2026-09-22), when the developer asked for the three links to become tabs
 * across the whole width, each with a small icon, after an application they
 * liked.
 *
 * <p>Drawn inline rather than taken from an icon library: three outlines do not
 * justify a dependency, and an inline SVG stroked in `currentColor` takes the
 * open tab's violet and the others' muted grey from the stylesheet with no rule
 * per icon. The shapes follow the outline style of Tabler Icons - a 24-unit
 * grid, a round 2-unit stroke, no fill - so they sit beside text the way the
 * rest of the application's lines do.
 *
 * <p>`aria-hidden`, because the word beside each is the whole of its meaning: a
 * screen reader saying "house, Acasă" would teach nobody anything, and the
 * link's accessible name must stay exactly the word the tests and the journey
 * look for.
 */
const SHAPES = {
  home: [
    'M5 12H3l9-9 9 9h-2',
    'M5 12v7a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-7',
    'M9 21v-6a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2v6',
  ],
  garage: [
    'M5 17a2 2 0 1 0 4 0a2 2 0 1 0-4 0',
    'M15 17a2 2 0 1 0 4 0a2 2 0 1 0-4 0',
    'M5 17H3v-6l2-5h9l4 5h1a2 2 0 0 1 2 2v4h-2m-4 0H9m-6-6h15m-6 0V6',
  ],
  // Tabler's bulb, since 1.1: the Sugestii tab is for ideas first.
  feedback: [
    'M3 12h1m8-9v1m8 8h1m-15.4-6.4l.7.7m12.1-.7l-.7.7',
    'M9 16a5 5 0 1 1 6 0a3.5 3.5 0 0 0-1 3a2 2 0 0 1-4 0a3.5 3.5 0 0 0-1-3',
    'M9.7 17h4.6',
  ],
  profile: [
    'M8 7a4 4 0 1 0 8 0a4 4 0 0 0-8 0',
    'M6 21v-2a4 4 0 0 1 4-4h4a4 4 0 0 1 4 4v2',
  ],
} as const

export type NavIconName = keyof typeof SHAPES

export function NavIcon({ name }: { readonly name: NavIconName }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      {SHAPES[name].map(path => <path key={path} d={path} />)}
    </svg>
  )
}
