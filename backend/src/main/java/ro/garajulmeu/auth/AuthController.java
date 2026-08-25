package ro.garajulmeu.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import ro.garajulmeu.auth.dto.ForgotPasswordRequest;
import ro.garajulmeu.auth.dto.LoginRequest;
import ro.garajulmeu.auth.dto.LoginResponse;
import ro.garajulmeu.auth.dto.RefreshRequest;
import ro.garajulmeu.auth.dto.RefreshResponse;
import ro.garajulmeu.auth.dto.RegisterRequest;
import ro.garajulmeu.auth.dto.ResendVerificationRequest;
import ro.garajulmeu.auth.dto.ResetPasswordRequest;
import ro.garajulmeu.auth.dto.VerifyEmailRequest;
import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;

	private final RefreshCookies refreshCookies;

	private final AuthRateLimit rateLimit;

	AuthController(AuthService authService, RefreshCookies refreshCookies, AuthRateLimit rateLimit) {
		this.authService = authService;
		this.refreshCookies = refreshCookies;
		this.rateLimit = rateLimit;
	}

	/**
	 * Answers 201 with an empty body. There is nothing useful to return: the
	 * account cannot be used until the emailed code is confirmed, and an empty
	 * response cannot leak anything about what was just created.
	 */
	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public void register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
		rateLimit.emailDispatch(httpRequest, request.email());
		authService.register(request);
	}

	@PostMapping("/verify-email")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void verifyEmail(@Valid @RequestBody VerifyEmailRequest request, HttpServletRequest httpRequest) {
		rateLimit.credentialCheck(httpRequest, request.email());
		authService.verifyEmail(request);
	}

	@PostMapping("/resend-verification")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void resendVerification(@Valid @RequestBody ResendVerificationRequest request,
			HttpServletRequest httpRequest) {
		rateLimit.emailDispatch(httpRequest, request.email());
		authService.resendVerificationCode(request);
	}

	/**
	 * Always 204, whether or not the address exists. Specification section 14
	 * requires non-disclosure here specifically: this endpoint needs no cooperation
	 * from the account holder, so a truthful answer would be a free membership
	 * oracle for anyone holding a list of addresses.
	 */
	@PostMapping("/forgot-password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest httpRequest) {
		rateLimit.emailDispatch(httpRequest, request.email());
		authService.forgotPassword(request);
	}

	@PostMapping("/reset-password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void resetPassword(@Valid @RequestBody ResetPasswordRequest request, HttpServletRequest httpRequest) {
		rateLimit.credentialCheck(httpRequest, request.email());
		authService.resetPassword(request);
	}

	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest,
			HttpServletResponse response) {
		rateLimit.credentialCheck(httpRequest, request.email());

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
	 *
	 * <p>
	 * <strong>Not rate limited, since 2026-08-25, and the reasoning is on
	 * {@link AuthRateLimit}.</strong> The only key available here is the network
	 * address, because the token has not been looked up yet - and a mobile
	 * application shares one carrier address between thousands of people while
	 * refreshing at every launch. The request no longer takes an
	 * {@code HttpServletRequest} at all, so that nothing can quietly start reading
	 * an address again without this comment being read first.
	 */
	@PostMapping("/refresh")
	public RefreshResponse refresh(@RequestBody(required = false) RefreshRequest request,
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

	/**
	 * Idempotent: logging out twice, or with no token at all, is not an error.
	 *
	 * <p>
	 * Deliberately not rate limited. Refusing a logout is worse than allowing one:
	 * it would leave a user who asked to end their session with a live one. The
	 * endpoint is cheap - a SHA-256 lookup and an update - and revoking anything
	 * requires already holding a valid 256-bit token.
	 */
	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(@RequestBody(required = false) RefreshRequest request,
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