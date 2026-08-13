package ro.garajulmeu.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * @param code        rejected here if it is not six digits, so malformed input
 *                    never costs an Argon2 comparison
 * @param newPassword carries the same 12-128 bound as registration. A length
 *                    policy applies to new passwords, and this is one - unlike
 *                    LoginRequest, where enforcing it would lock existing users
 *                    out the day the policy tightens
 */
public record ResetPasswordRequest(

		@NotBlank
		@Email
		String email,

		@NotBlank
		@Pattern(regexp = "\\d{6}")
		String code,

		@NotBlank
		@Size(min = 12, max = 128)
		String newPassword) {
}