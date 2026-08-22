package ro.garajulmeu.user;

import java.util.UUID;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ro.garajulmeu.auth.AuthService;
import ro.garajulmeu.auth.RefreshCookies;
import ro.garajulmeu.user.dto.ChangeEmailRequest;
import ro.garajulmeu.user.dto.ChangePasswordRequest;
import ro.garajulmeu.user.dto.ConfirmEmailChangeRequest;
import ro.garajulmeu.user.dto.DeleteAccountRequest;
import ro.garajulmeu.user.dto.UpdateProfileRequest;
import ro.garajulmeu.user.dto.UserProfileResponse;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserService userService;

	/**
	 * The email change lives in AuthService because it is a verification-code
	 * flow, and the single implementation of that check is private there. The URL
	 * belongs here because the resource is the account. A controller coordinating
	 * two services is a smaller price than a second copy of the code check.
	 */
	private final AuthService authService;

	/** Only to clear the cookie on deletion; the path attribute is ours to write. */
	private final RefreshCookies refreshCookies;

	/**
	 * <strong>A valid token is a weaker guard than it looks.</strong> Until
	 * 2026-08-23 nothing under this controller was limited at all, on the
	 * reasoning that the caller must already hold an unexpired access token for
	 * this exact account. That reasoning holds right up until a token is stolen,
	 * and then it holds not at all: a thief could spend the account's mail
	 * reputation by requesting address changes in a loop, or burn CPU two Argon2
	 * operations at a time, for as long as the token lasts.
	 *
	 * <p>The limit is applied in the controller rather than the service, and
	 * before anything else, so that a refused attempt costs a map lookup instead
	 * of the hash it was trying to make us compute. A limit enforced after the
	 * expensive part would be decoration.
	 */
	private final AccountRateLimit rateLimit;

	UserController(UserService userService, AuthService authService, RefreshCookies refreshCookies,
			AccountRateLimit rateLimit) {
		this.userService = userService;
		this.authService = authService;
		this.refreshCookies = refreshCookies;
		this.rateLimit = rateLimit;
	}

	/**
	 * The identity comes from the verified token, never from a path or query
	 * parameter. That is what makes it impossible to ask for somebody else's
	 * profile by changing a value in the URL.
	 *
	 * <p>Deliberately not rate limited. It costs one indexed read and sends
	 * nothing; throttling it would only break a client that polls.
	 */
	@GetMapping("/me")
	public UserProfileResponse me(@AuthenticationPrincipal Jwt token) {
		return userService.profileOf(UUID.fromString(token.getSubject()));
	}

	/**
	 * Same rule: the account updated is the token's, and there is no way to name
	 * another. Also deliberately unlimited - it is a write, but a cheap one, and
	 * the trigger recorded for this work was "costs a hash or sends mail".
	 */
	@PatchMapping("/me")
	public UserProfileResponse updateMe(@AuthenticationPrincipal Jwt token,
			@Valid @RequestBody UpdateProfileRequest request) {
		return userService.updateProfile(UUID.fromString(token.getSubject()), request);
	}

	/**
	 * Answers 204 rather than a new token pair. Every session has just ended,
	 * including this one, so the honest next step is for the client to log in
	 * again with the password it just chose.
	 *
	 * <p>The most expensive endpoint on the controller: a comparison against the
	 * stored hash and an encode of the replacement, two Argon2 operations at
	 * roughly 50 ms and 16 MB each.
	 */
	@PostMapping("/me/change-password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void changePassword(@AuthenticationPrincipal Jwt token,
			@Valid @RequestBody ChangePasswordRequest request) {
		UUID accountId = UUID.fromString(token.getSubject());
		rateLimit.credentialCheck(accountId);
		userService.changePassword(accountId, request);
	}

	/**
	 * 204: the code has been sent to the address on file, and nothing has changed
	 * yet.
	 *
	 * <p>Limited by {@code emailDispatch} rather than {@code credentialCheck},
	 * although it pays a hash too. Both costs are real and the tighter policy
	 * wins: CPU we can buy, and a sending domain burnt by loops of mail we
	 * cannot.
	 */
	@PostMapping("/me/change-email")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void changeEmail(@AuthenticationPrincipal Jwt token,
			@Valid @RequestBody ChangeEmailRequest request) {
		UUID accountId = UUID.fromString(token.getSubject());
		rateLimit.emailDispatch(accountId);
		authService.requestEmailChange(accountId, request.newEmail(), request.currentPassword());
	}

	/**
	 * Returns the profile rather than 204, because the account has just changed in
	 * two ways at once - a new address, and {@code emailVerified} back to false.
	 * Sending it saves a round trip and makes the state the client must react to
	 * impossible to miss.
	 *
	 * <p>Limited because the code is stored hashed and checking one costs a
	 * comparison - the same reason the six-digit codes on the auth endpoints are
	 * limited. Six digits is a million guesses, which is nothing without a limit.
	 */
	@PostMapping("/me/confirm-email-change")
	public UserProfileResponse confirmEmailChange(@AuthenticationPrincipal Jwt token,
			@Valid @RequestBody ConfirmEmailChangeRequest request) {
		UUID accountId = UUID.fromString(token.getSubject());
		rateLimit.credentialCheck(accountId);
		authService.confirmEmailChange(accountId, request.code());
		return userService.profileOf(accountId);
	}

	/**
	 * Carries a body, which DELETE is permitted but not required to do. The
	 * password has to travel somewhere, and a query parameter would put it in
	 * access logs and browser history - the two places a password must never be.
	 *
	 * <p>The refresh cookie is cleared on the way out. Its row has just been
	 * deleted, so it is already inert, but leaving a dead credential in the
	 * browser of somebody who has just erased their account is untidy at best.
	 *
	 * <p>Limited for the hash it pays. The budget it draws on is shared with the
	 * password change, and that is correct: a caller who has just been refused
	 * for grinding passwords has no business grinding them here instead.
	 */
	@DeleteMapping("/me")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteMe(@AuthenticationPrincipal Jwt token,
			@Valid @RequestBody DeleteAccountRequest request,
			HttpServletResponse response) {
		UUID accountId = UUID.fromString(token.getSubject());
		rateLimit.credentialCheck(accountId);
		userService.deleteAccount(accountId, request);
		response.addHeader(HttpHeaders.SET_COOKIE, refreshCookies.clear().toString());
	}
}