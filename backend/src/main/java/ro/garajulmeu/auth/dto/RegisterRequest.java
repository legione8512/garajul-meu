package ro.garajulmeu.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * The body of POST /api/v1/auth/register.
 *
 * <p>The language is a plain string rather than the {@code Language} enum: JSON
 * carries the lower-case tag "ro", while Jackson would expect the enum constant
 * name "RO". Converting in the service keeps the wire format identical to the
 * one i18next uses on the frontend.
 *
 * @param password minimum twelve characters, with no composition rules. Current
 *                 guidance favours length over forced symbols, which mostly
 *                 push people towards predictable substitutions. The maximum
 *                 bounds the work Argon2 is asked to do per request.
 */
public record RegisterRequest(

		@NotBlank
		@Size(max = 120)
		String fullName,

		@NotBlank
		@Email
		@Size(max = 320)
		String email,

		@NotBlank
		@Size(min = 12, max = 128)
		String password,

		@Pattern(regexp = "ro|en")
		String preferredLanguage) {
}