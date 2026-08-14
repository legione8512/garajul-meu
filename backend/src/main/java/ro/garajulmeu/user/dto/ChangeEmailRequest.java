package ro.garajulmeu.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param newEmail        the address to move the account to. It is confirmed by
 *                        a code sent to the address currently on file, never to
 *                        this one
 * @param currentPassword defence in depth rather than the primary guard. The
 *                        code going only to the old address is already what
 *                        defeats a stolen access token; requiring the password
 *                        additionally means a token alone cannot even provoke
 *                        the email. No length bound, for the same reason
 *                        {@code ChangePasswordRequest.currentPassword} carries
 *                        none: a length policy applies to new passwords
 */
public record ChangeEmailRequest(

		@NotBlank
		@Email
		@Size(max = 320)
		String newEmail,

		@NotBlank
		String currentPassword) {
}