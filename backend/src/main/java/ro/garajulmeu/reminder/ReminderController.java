package ro.garajulmeu.reminder;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ro.garajulmeu.reminder.dto.ReminderView;

/**
 * Specification section 16's one reminder endpoint.
 *
 * <p>Its own controller rather than a sixth method on
 * {@code VehicleDocumentController}. Section 4 organises packages by domain, and
 * a path sitting under {@code /documents/} says where the resource is reached,
 * not who owns it - the same reasoning that keeps renewal and history on their
 * own paths rather than as modes of the document endpoints.
 *
 * <p>Read-only, and there is deliberately nothing else here. A reminder is not
 * something a person creates, edits or deletes: it exists because a document has
 * a date and an account has preferences, and both are changed elsewhere. An
 * endpoint to delete one would put the table out of agreement with the two
 * things that decide it.
 */
@RestController
@RequestMapping("/api/v1/vehicles/{vehicleId}/documents/{documentId}/reminders")
public class ReminderController {

	private final ReminderService reminderService;

	ReminderController(ReminderService reminderService) {
		this.reminderService = reminderService;
	}

	@GetMapping
	public List<ReminderView> schedule(@AuthenticationPrincipal Jwt token,
			@PathVariable UUID vehicleId, @PathVariable UUID documentId) {
		return reminderService.scheduleOf(accountOf(token), vehicleId, documentId);
	}

	private static UUID accountOf(Jwt token) {
		return UUID.fromString(token.getSubject());
	}
}