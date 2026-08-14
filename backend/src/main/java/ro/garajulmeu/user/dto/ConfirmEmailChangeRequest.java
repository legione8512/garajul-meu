package ro.garajulmeu.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * @param code six digits. The pattern rejects malformed input before it costs an
 *             Argon2 comparison, exactly as the verification and reset DTOs do
 */
public record ConfirmEmailChangeRequest(

		@NotBlank
		@Pattern(regexp = "\\d{6}")
		String code) {
}