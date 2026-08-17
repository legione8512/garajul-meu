package ro.garajulmeu.notification;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ro.garajulmeu.notification.dto.NotificationPreferencesView;
import ro.garajulmeu.notification.dto.SaveNotificationPreferencesRequest;

/**
 * Specification section 16's two preference endpoints. Under {@code /users/me},
 * so the only account either can describe is the token's.
 */
@RestController
@RequestMapping("/api/v1/users/me/notification-preferences")
public class NotificationPreferencesController {

	private final NotificationPreferencesService preferencesService;

	NotificationPreferencesController(NotificationPreferencesService preferencesService) {
		this.preferencesService = preferencesService;
	}

	@GetMapping
	public NotificationPreferencesView preferences(@AuthenticationPrincipal Jwt token) {
		return preferencesService.of(accountOf(token));
	}

	@PutMapping
	public NotificationPreferencesView replace(@AuthenticationPrincipal Jwt token,
			@Valid @RequestBody SaveNotificationPreferencesRequest request) {
		return preferencesService.replace(accountOf(token), request);
	}

	private static UUID accountOf(Jwt token) {
		return UUID.fromString(token.getSubject());
	}
}