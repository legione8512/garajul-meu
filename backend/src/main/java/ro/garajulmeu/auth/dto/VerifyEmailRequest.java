package ro.garajulmeu.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * The body of POST /api/v1/auth/verify-email.
 *
 * @param code exactly six digits. Rejecting anything else before it reaches the
 *             service means malformed input never costs an Argon2 comparison,
 *             which is the expensive part of this endpoint.
 */
public record VerifyEmailRequest(

		@NotBlank
		@Email
		String email,

		@NotBlank
		@Pattern(regexp = "\\d{6}")
		String code) {
}