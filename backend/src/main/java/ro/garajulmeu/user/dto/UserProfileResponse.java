package ro.garajulmeu.user.dto;

import java.util.UUID;

/**
 * The body of GET /api/v1/users/me, per specification section 16.
 *
 * <p>A response DTO rather than the entity: exposing {@code User} directly would
 * put {@code passwordHash} one Jackson change away from the wire.
 */
public record UserProfileResponse(
		UUID id,
		String fullName,
		String email,
		String preferredLanguage,
		String timezone,
		boolean emailVerified) {
}