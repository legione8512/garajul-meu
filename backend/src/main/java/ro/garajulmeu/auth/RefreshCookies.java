package ro.garajulmeu.auth;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Builds the refresh cookie used by the web client.
 *
 * <p>{@code HttpOnly} keeps the token out of reach of JavaScript, which is the
 * whole point of preferring a cookie over local storage on the web.
 *
 * <p>{@code SameSite=Strict} is the CSRF defence: a browser will not attach this
 * cookie to a request originating from another site, so a forged POST arrives
 * with no credentials at all. Browsers treat {@code http://localhost} as a
 * secure context, so {@code Secure} does not hinder development.
 *
 * <p>The path confines it to the authentication endpoints. No other request ever
 * carries the refresh token, which limits where it can leak.
 */
@Component
public class RefreshCookies {

	public static final String NAME = "garajul_meu_refresh";

	private static final String PATH = "/api/v1/auth";

	private final AuthProperties properties;

	RefreshCookies(AuthProperties properties) {
		this.properties = properties;
	}

	public ResponseCookie issue(String value) {
		return base(value).maxAge(properties.refreshTokenValidity()).build();
	}

	/** An empty value with a zero lifetime is how a cookie is deleted. */
	public ResponseCookie clear() {
		return base("").maxAge(Duration.ZERO).build();
	}

	private ResponseCookie.ResponseCookieBuilder base(String value) {
		return ResponseCookie.from(NAME, value)
				.httpOnly(true)
				.secure(true)
				.sameSite("Strict")
				.path(PATH);
	}
}