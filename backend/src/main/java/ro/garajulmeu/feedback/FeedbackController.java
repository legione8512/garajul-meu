package ro.garajulmeu.feedback;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ro.garajulmeu.feedback.dto.SendFeedbackRequest;

/**
 * The Sugestii tab's one endpoint, 1.1. Signed-in accounts only, like every
 * path outside {@code /api/v1/auth}: the reply goes to the account's address,
 * and an open form would be a spam relay to the operator's inbox.
 */
@RestController
@RequestMapping("/api/v1/feedback")
public class FeedbackController {

	private final FeedbackService feedbackService;

	FeedbackController(FeedbackService feedbackService) {
		this.feedbackService = feedbackService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void send(@AuthenticationPrincipal Jwt token, @Valid @RequestBody SendFeedbackRequest request) {
		feedbackService.send(UUID.fromString(token.getSubject()), request);
	}
}
