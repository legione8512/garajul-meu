package ro.garajulmeu.user;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}