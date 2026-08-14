package ro.garajulmeu.user;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ro.garajulmeu.auth.AuthService;
import ro.garajulmeu.user.dto.ChangeEmailRequest;
import ro.garajulmeu.user.dto.ChangePasswordRequest;
import ro.garajulmeu.user.dto.ConfirmEmailChangeRequest;
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

	UserController(UserService userService, AuthService authService) {
		this.userService = userService;
		this.authService = authService;
	}

	/**
	 * The identity comes from the verified token, never from a path or query
	 * parameter. That is what makes it impossible to ask for somebody else's
	 * profile by changing a value in the URL.
	 */
	@GetMapping("/me")
	public UserProfileResponse me(@AuthenticationPrincipal Jwt token) {
		return userService.profileOf(UUID.fromString(token.getSubject()));
	}

	/** Same rule: the account updated is the token's, and there is no way to name another. */
	@PatchMapping("/me")
	public UserProfileResponse updateMe(@AuthenticationPrincipal Jwt token,
			@Valid @RequestBody UpdateProfileRequest request) {
		return userService.updateProfile(UUID.fromString(token.getSubject()), request);
	}

	/**
	 * Answers 204 rather than a new token pair. Every session has just ended,
	 * including this one, so the honest next step is for the client to log in
	 * again with the password it just chose.
	 */
	@PostMapping("/me/change-password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void changePassword(@AuthenticationPrincipal Jwt token,
			@Valid @RequestBody ChangePasswordRequest request) {
		userService.changePassword(UUID.fromString(token.getSubject()), request);
	}

	/** 204: the code has been sent to the address on file, and nothing has changed yet. */
	@PostMapping("/me/change-email")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void changeEmail(@AuthenticationPrincipal Jwt token,
			@Valid @RequestBody ChangeEmailRequest request) {
		authService.requestEmailChange(UUID.fromString(token.getSubject()),
				request.newEmail(), request.currentPassword());
	}

	/**
	 * Returns the profile rather than 204, because the account has just changed in
	 * two ways at once - a new address, and {@code emailVerified} back to false.
	 * Sending it saves a round trip and makes the state the client must react to
	 * impossible to miss.
	 */
	@PostMapping("/me/confirm-email-change")
	public UserProfileResponse confirmEmailChange(@AuthenticationPrincipal Jwt token,
			@Valid @RequestBody ConfirmEmailChangeRequest request) {
		UUID accountId = UUID.fromString(token.getSubject());
		authService.confirmEmailChange(accountId, request.code());
		return userService.profileOf(accountId);
	}
}