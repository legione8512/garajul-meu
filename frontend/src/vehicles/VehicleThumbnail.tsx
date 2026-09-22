import { useVehicleThumbnail } from './useVehicleImage.ts'

interface Props {
  readonly vehicleId: string
  /** What the card was told. False asks for nothing and draws the empty circle. */
  readonly hasImage: boolean
}

/**
 * The owner's own photograph, round, to the left of the vehicle's name on the
 * dashboard and garage cards. Asked for on 2026-09-22: "o iconiță cu poza
 * mașinii", with a placeholder where there is no picture.
 *
 * <p>The card was already carrying the make's emblem on the right, and the two
 * say different things - the emblem is what the vehicle is, this is which one it
 * is. A person with two Logans knows them apart by their colour long before they
 * read a plate.
 *
 * <p><strong>A circle of fixed size either way</strong>, and that is the whole
 * layout decision: the picture arrives after the card is on screen, and a space
 * that grows when it lands would push the name down under the reader's eye. The
 * empty state is the same circle with a camera in it rather than nothing at all,
 * so a garage of five cars stays a column of five identical shapes whether they
 * are photographed or not.
 *
 * <p><strong>Failures draw the placeholder and say nothing.</strong> A thumbnail
 * that could not be made or could not be fetched is not something the reader can
 * act on, and an alert on a dashboard card for a decoration would be noise in the
 * one place the application is supposed to be calm. The screen that manages the
 * photograph reports its errors properly.
 *
 * <p>`aria-hidden` for the same reason as the emblem: the card already names the
 * vehicle in words, and the picture is the owner's own. The stretched card link
 * must also stay the one link a screen reader hears.
 */
export function VehicleThumbnail({ vehicleId, hasImage }: Props) {
  const { url } = useVehicleThumbnail(vehicleId, hasImage)

  return (
    <span data-thumbnail={url === null ? 'empty' : 'photo'} aria-hidden="true">
      {url === null
        ? (
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth={2}
            strokeLinecap="round"
            strokeLinejoin="round"
            focusable="false"
          >
            <path d="M5 7h1a2 2 0 0 0 2-2a1 1 0 0 1 1-1h6a1 1 0 0 1 1 1a2 2 0 0 0 2 2h1a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2h-14a2 2 0 0 1-2-2v-9a2 2 0 0 1 2-2" />
            <path d="M9 13a3 3 0 1 0 6 0a3 3 0 0 0-6 0" />
          </svg>
          )
        : <img src={url} alt="" />}
    </span>
  )
}
