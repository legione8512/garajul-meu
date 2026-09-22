package ro.garajulmeu.vehicle;

import java.util.Locale;
import java.util.Optional;

/**
 * How a vehicle is used - added in 1.0.2 (2026-09-22) at the owner's request,
 * after a similar application that asks for it, and deliberately **plain
 * information for now**: nothing in the application reads it yet.
 *
 * <p>It is stored rather than left out because it is the fact later features
 * will want. The obvious one is the ITP interval, which in Romania depends on
 * how a car is used - a taxi or a passenger-transport vehicle is inspected far
 * more often than a private car - and a suggestion of the next ITP date could
 * not be made without it. The owner named that direction and chose not to build
 * it yet.
 *
 * <p>Not a certificate field: the registration certificate does not state it,
 * so it lives on the vehicle beside the nickname, as something its owner
 * declares. Stored as text, as document types are, so a fourth use is a new
 * constant rather than a migration.
 */
public enum VehicleUsage {

	/** Ordinary private use - the default, and what nearly every car is. */
	NORMAL,

	/** Licensed taxi. */
	TAXI,

	/** Passenger or goods transport as a business. */
	TRANSPORT;

	/**
	 * Reads a use from what a client sent, or nothing - the shape of
	 * {@code DocumentType.of}, for the reason it gives.
	 */
	public static Optional<VehicleUsage> of(String raw) {
		if (raw == null) {
			return Optional.empty();
		}

		String cleaned = raw.trim().toUpperCase(Locale.ROOT);

		for (VehicleUsage usage : values()) {
			if (usage.name().equals(cleaned)) {
				return Optional.of(usage);
			}
		}
		return Optional.empty();
	}
}
