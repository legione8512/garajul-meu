package ro.garajulmeu.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * @param refreshTokenInBody native clients have no cookie jar and ask for the
 *                           refresh token in the response body. Deliberately
 *                           boxed: Jackson 3 enables
 *                           {@code FAIL_ON_NULL_FOR_PRIMITIVES} by default, so
 *                           an absent property mapped onto a {@code boolean}
 *                           makes the entire request unreadable rather than
 *                           defaulting to false, as Jackson 2 did. Boxed, an
 *                           absent property is simply null.
 */
public record LoginRequest(

		@NotBlank
		@Email
		String email,

		@NotBlank
		String password,

		Boolean refreshTokenInBody) {

	/**
	 * Absent, null and false all mean the same thing: issue the refresh token as
	 * an HttpOnly cookie only, so a browser's JavaScript never sees it. Only an
	 * explicit true opts into the body. Keeping that judgement here means the
	 * controller never has to remember what null meant.
	 */
	public boolean wantsRefreshTokenInBody() {
		return Boolean.TRUE.equals(refreshTokenInBody);
	}
}