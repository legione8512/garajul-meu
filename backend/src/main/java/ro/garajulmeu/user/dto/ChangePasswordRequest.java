package ro.garajulmeu.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param currentPassword deliberately carries no length policy. It was accepted
 *                        under whatever rules applied when it was chosen, and
 *                        enforcing today's minimum here would stop exactly the
 *                        people who most need to change it
 * @param newPassword     12-128, the same bound as registration and reset
 */
public record ChangePasswordRequest(

		@NotBlank
		String currentPassword,

		@NotBlank
		@Size(min = 12, max = 128)
		String newPassword) {
}