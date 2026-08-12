package ro.garajulmeu.auth;

import jakarta.validation.Valid;

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

	AuthController(AuthService authService) {
		this.authService = authService;
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
}