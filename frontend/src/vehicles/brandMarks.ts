import {
  siAudi, siBmw, siCadillac, siChevrolet, siCitroen, siDacia, siDsautomobiles, siDucati,
  siFiat, siFord, siHonda, siHyundai, siInfiniti, siIveco, siJeep, siKia, siKtm, siLada,
  siMaserati, siMazda, siMg, siMini, siMitsubishi, siNissan, siOpel, siPeugeot, siPolestar,
  siPorsche, siRenault, siSeat, siSkoda, siSmart, siSubaru, siSuzuki, siTesla, siToyota,
  siVespa, siVolkswagen, siVolvo,
  type SimpleIcon,
} from 'simple-icons'

import { brandCandidates } from './brandKey.ts'

/**
 * What a card shows for a make: the brand's emblem as a 24x24 path, or - for a
 * brand recognised but without an emblem here - its name, set in type.
 */
export type BrandMark =
  | { readonly kind: 'emblem', readonly title: string, readonly path: string }
  | { readonly kind: 'name', readonly name: string }

/**
 * The brands that can show an emblem, keyed by `brandCandidates` spelling.
 *
 * <p><strong>Asked for by the developer on 2026-09-14</strong>, and outside
 * specification V1: an emblem on the right of each vehicle card on the dashboard
 * and in the garage, and nothing at all when the make is not recognised.
 *
 * <p><strong>From Simple Icons</strong> (the `simple-icons` package, CC0): one
 * path per brand, drawn in one colour, which is what lets an emblem sit on a dark
 * card at all - a navy Volvo or Ford in its own colours would vanish there. The
 * emblems are the manufacturers' trademarks. They are shown to name the make of
 * the person's own vehicle, and Simple Icons' DISCLAIMER.md asks each project to
 * judge its own use; that judgement is recorded in docs/PROJECT_STATE.md.
 *
 * <p><strong>A chosen list, not the whole catalogue.</strong> The cars, vans
 * and motorcycles a Romanian garage plausibly holds, checked against version
 * 16.31.0 on 2026-09-14. Left out on purpose, for weight in a module every
 * dashboard loads: Ferrari (20 KB), Bentley (15 KB), Lamborghini, Aston Martin,
 * Rolls-Royce, Bugatti, McLaren, Chrysler, and the lorry makers. Not in Simple
 * Icons at all, most of them removed at the brand's request: Mercedes-Benz,
 * Land Rover, Jaguar, Lexus, Alfa Romeo, Cupra, BYD, Lancia, Saab, SsangYong,
 * Daewoo. The cars among both lists are in {@link NAMES} instead.
 *
 * <p>Imported by name, so the bundler keeps these 39 paths and drops the other
 * three thousand icons; and the module itself is loaded only by
 * `useBrandMark`, so a visitor to the landing page downloads none of it.
 */
const MARKS: ReadonlyMap<string, SimpleIcon> = new Map([
  ['AUDI', siAudi],
  ['BMW', siBmw],
  ['CADILLAC', siCadillac],
  ['CHEVROLET', siChevrolet],
  ['CITROEN', siCitroen],
  ['DACIA', siDacia],
  ['DS', siDsautomobiles],
  ['DSAUTOMOBILES', siDsautomobiles],
  ['DUCATI', siDucati],
  ['FIAT', siFiat],
  ['FORD', siFord],
  ['HONDA', siHonda],
  ['HYUNDAI', siHyundai],
  ['INFINITI', siInfiniti],
  ['IVECO', siIveco],
  ['JEEP', siJeep],
  ['KIA', siKia],
  ['KTM', siKtm],
  ['LADA', siLada],
  ['MASERATI', siMaserati],
  ['MAZDA', siMazda],
  ['MG', siMg],
  ['MINI', siMini],
  ['MITSUBISHI', siMitsubishi],
  ['NISSAN', siNissan],
  ['OPEL', siOpel],
  ['PEUGEOT', siPeugeot],
  ['POLESTAR', siPolestar],
  ['PORSCHE', siPorsche],
  ['RENAULT', siRenault],
  ['SEAT', siSeat],
  ['SKODA', siSkoda],
  ['SMART', siSmart],
  ['SUBARU', siSubaru],
  ['SUZUKI', siSuzuki],
  ['TESLA', siTesla],
  ['TOYOTA', siToyota],
  ['VESPA', siVespa],
  ['VOLKSWAGEN', siVolkswagen],
  ['VW', siVolkswagen],
  ['VOLVO', siVolvo],
])

/**
 * Brands recognised without an emblem, and the name each card sets in type in
 * its place.
 *
 * <p>Asked for by the developer on 2026-09-14, the moment the missing emblems
 * were listed: a Mercedes-Benz card with an empty corner, beside a Dacia card
 * with an emblem, reads as a fault rather than as a rule. The rule for a make
 * nobody recognises is unchanged - nothing.
 */
const NAMES: ReadonlyMap<string, string> = new Map([
  ['ALFAROMEO', 'Alfa Romeo'],
  ['ASTONMARTIN', 'Aston Martin'],
  ['BENTLEY', 'Bentley'],
  ['BUGATTI', 'Bugatti'],
  ['BYD', 'BYD'],
  ['CHRYSLER', 'Chrysler'],
  ['CUPRA', 'Cupra'],
  ['DAEWOO', 'Daewoo'],
  ['DODGE', 'Dodge'],
  ['FERRARI', 'Ferrari'],
  ['JAGUAR', 'Jaguar'],
  ['LAMBORGHINI', 'Lamborghini'],
  ['LANCIA', 'Lancia'],
  ['LANDROVER', 'Land Rover'],
  ['LEXUS', 'Lexus'],
  ['MCLAREN', 'McLaren'],
  ['MERCEDES', 'Mercedes-Benz'],
  ['MERCEDESBENZ', 'Mercedes-Benz'],
  ['ROLLSROYCE', 'Rolls-Royce'],
  ['SAAB', 'Saab'],
  ['SSANGYONG', 'SsangYong'],
])

/**
 * What to show for a make: its emblem, else its name, else `null` when the make
 * names no brand either table knows. Each spelling is tried in both tables
 * before the next, so the most specific reading of the make always wins.
 */
export function markFor(make: string): BrandMark | null {
  for (const candidate of brandCandidates(make)) {
    const icon = MARKS.get(candidate)

    if (icon !== undefined) {
      return { kind: 'emblem', title: icon.title, path: icon.path }
    }

    const name = NAMES.get(candidate)

    if (name !== undefined) {
      return { kind: 'name', name }
    }
  }

  return null
}