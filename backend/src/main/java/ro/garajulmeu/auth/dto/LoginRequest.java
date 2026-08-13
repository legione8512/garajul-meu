package ro.garajulmeu.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * @param refreshTokenInBody native clients have no cookie jar and ask for the
 *                           refresh token in the response body. Absent means
 *                           false, so a browser gets the HttpOnly cookie only
 *                           and its JavaScript never sees the token.
 */
public record LoginRequest(

		@NotBlank
		@Email
		String email,

		@NotBlank
		String password,

		boolean refreshTokenInBody) {
}