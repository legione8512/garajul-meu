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

import ro.garajulmeu.user.dto.ChangePasswordRequest;
import ro.garajulmeu.user.dto.UpdateProfileRequest;
import ro.garajulmeu.user.dto.UserProfileResponse;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserService userService;

	UserController(UserService userService) {
		this.userService = userService;
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
}