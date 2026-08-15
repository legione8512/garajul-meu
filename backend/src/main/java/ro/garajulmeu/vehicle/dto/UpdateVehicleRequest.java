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
 */
public record UpdateVehicleRequest(@Size(max = 120) String displayName) {
}