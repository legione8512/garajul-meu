package ro.garajulmeu.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @param currentPassword required because deletion cannot be undone. Of the
 *                        three endpoints that ask for the current password this
 *                        is the one that most needs it: an access token stolen
 *                        for fifteen minutes must not be enough to destroy an
 *                        account that no support process can restore. No length
 *                        bound, for the same reason as elsewhere - the policy
 *                        applies to new passwords
 */
public record DeleteAccountRequest(

		@NotBlank
		String currentPassword) {
}