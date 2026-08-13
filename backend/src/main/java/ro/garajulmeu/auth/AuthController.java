package ro.garajulmeu.auth;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.CookieValue;

import ro.garajulmeu.auth.dto.RefreshRequest;
import ro.garajulmeu.auth.dto.RefreshResponse;
import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;
import ro.garajulmeu.auth.dto.LoginRequest;
import ro.garajulmeu.auth.dto.LoginResponse;
import ro.garajulmeu.auth.dto.ResendVerificationRequest;
import ro.garajulmeu.auth.dto.VerifyEmailRequest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ro.garajulmeu.auth.dto.RegisterRequest;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;

	private final RefreshCookies refreshCookies;

	AuthController(AuthService authService, RefreshCookies refreshCookies) {
		this.authService = authService;
		this.refreshCookies = refreshCookies;
	}

	/**
	 * Answers 201 with an empty body. There is nothing useful to return: the
	 * account cannot be used until the emailed code is confirmed, and an empty
	 * response cannot leak anything about what was just created.
	 */
	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public void register(@Valid @RequestBody RegisterRequest request) {
		authService.register(request);
	}
	@PostMapping("/verify-email")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
		authService.verifyEmail(request);
	}

	@PostMapping("/resend-verification")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
		authService.resendVerificationCode(request);
	}
	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
		AuthService.LoginResult result = authService.login(request);

		// The cookie is always set. A native client simply ignores it; a browser
		// relies on it and never sees the token in the body.
		response.addHeader(HttpHeaders.SET_COOKIE, refreshCookies.issue(result.refreshToken()).toString());

		return new LoginResponse(result.accessToken(), result.expiresInSeconds(),
				request.wantsRefreshTokenInBody() ? result.refreshToken() : null);
	}

	/**
	 * Accepts the refresh token from the body or from the cookie, and answers on
	 * whichever channel it arrived. Specification section 14 forbids requiring a
	 * client-type header, and none is needed: the request itself says how this
	 * client works.
	 */
	@PostMapping("/refresh")
	public RefreshResponse refresh(
			@RequestBody(required = false) RefreshRequest request,
			@CookieValue(name = RefreshCookies.NAME, required = false) String cookieToken,
			HttpServletResponse response) {

		boolean explicit = request != null && request.refreshToken() != null && !request.refreshToken().isBlank();
		String presented = explicit ? request.refreshToken() : cookieToken;

		if (presented == null || presented.isBlank()) {
			throw new ApiException(ErrorCode.REFRESH_TOKEN_INVALID);
		}

		AuthService.LoginResult result = authService.refresh(presented);

		if (explicit) {
			return new RefreshResponse(result.accessToken(), result.expiresInSeconds(), result.refreshToken());
		}

		response.addHeader(HttpHeaders.SET_COOKIE, refreshCookies.issue(result.refreshToken()).toString());
		return new RefreshResponse(result.accessToken(), result.expiresInSeconds(), null);
	}

	/** Idempotent: logging out twice, or with no token at all, is not an error. */
	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(
			@RequestBody(required = false) RefreshRequest request,
			@CookieValue(name = RefreshCookies.NAME, required = false) String cookieToken,
			HttpServletResponse response) {

		String presented = (request != null && request.refreshToken() != null && !request.refreshToken().isBlank())
				? request.refreshToken()
				: cookieToken;

		if (presented != null && !presented.isBlank()) {
			authService.logout(presented);
		}

		response.addHeader(HttpHeaders.SET_COOKIE, refreshCookies.clear().toString());
	}
}