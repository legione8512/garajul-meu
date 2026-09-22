package ro.garajulmeu.vehicle.dto;

import jakarta.validation.constraints.Size;

/**
 * A partial update, following the same convention as PATCH /users/me: a field
 * the body does not mention is left alone.
 *
 * <p>That convention has a gap for an optional value - if absent means "leave
 * alone", nothing means "remove". Here a nickname sent as an empty or
 * whitespace-only string clears it. Explicit, testable, and it keeps null
 * meaning the one thing it means everywhere else in this API.
 *
 * <p>{@code usageType} since 1.0.2: NORMAL, TAXI or TRANSPORT, and never
 * cleared - a vehicle always has a use - so absent is the only way to leave it
 * alone, and anything else must be one of the three.
 */
public record UpdateVehicleRequest(@Size(max = 120) String displayName, String usageType) {
}
