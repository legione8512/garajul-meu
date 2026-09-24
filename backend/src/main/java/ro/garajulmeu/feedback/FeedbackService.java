package ro.garajulmeu.feedback;

import java.time.Clock;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.garajulmeu.email.EmailProvider;
import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;
import ro.garajulmeu.feedback.dto.SendFeedbackRequest;
import ro.garajulmeu.user.User;
import ro.garajulmeu.user.UserRepository;

/**
 * The Sugestii tab, 1.1: a message is stored, then emailed to the operator with
 * the sender's address as its Reply-To.
 *
 * <p><strong>A mail that fails does not lose the message.</strong> The row is
 * written first and the send is attempted after it, and a failure to send is
 * logged rather than thrown: the person is told their message arrived, which
 * is true - it is in the database, where the operator can still read it.
 * Nothing about the message's content is ever logged, only its identifier.
 */
@Service
public class FeedbackService {

	private static final Logger log = LoggerFactory.getLogger(FeedbackService.class);

	private static final Set<String> PLATFORMS = Set.of("WEB", "ANDROID", "IOS");

	private final FeedbackRepository feedbackRepository;
	private final UserRepository userRepository;
	private final EmailProvider emailProvider;
	private final FeedbackProperties properties;
	private final Clock clock;

	FeedbackService(FeedbackRepository feedbackRepository, UserRepository userRepository,
			EmailProvider emailProvider, FeedbackProperties properties, Clock clock) {
		this.feedbackRepository = feedbackRepository;
		this.userRepository = userRepository;
		this.emailProvider = emailProvider;
		this.properties = properties;
		this.clock = clock;
	}

	@Transactional
	public void send(UUID accountId, SendFeedbackRequest request) {
		FeedbackCategory category = FeedbackCategory.of(request.category())
				.orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_ERROR));
		String message = request.message().strip();

		// Twenty-four hours rather than a calendar day: an allowance that resets
		// at midnight allows ten in two minutes either side of it.
		long recent = feedbackRepository.countByUserIdAndCreatedAtAfter(accountId,
				clock.instant().minus(Duration.ofDays(1)));
		if (recent >= properties.dailyLimit()) {
			throw new ApiException(ErrorCode.FEEDBACK_LIMIT_REACHED);
		}

		User user = userRepository.findById(accountId)
				.orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

		String platform = platformOf(request.platform());
		String appVersion = platform == null ? null : trimmedOrNull(request.appVersion());

		FeedbackMessage saved = feedbackRepository.saveAndFlush(
				new FeedbackMessage(accountId, category, message, platform, appVersion));

		FeedbackMail.Mail mail = FeedbackMail.of(category, message, user.getFullName(),
				user.getEmail(), platform, appVersion, clock.instant());

		try {
			emailProvider.sendFeedback(properties.recipient(), user.getEmail(), mail.subject(),
					mail.body());
			log.info("Feedback {} ({}) stored and emailed", saved.getId(), category);
		}
		catch (RuntimeException failure) {
			log.warn("Feedback {} ({}) stored but not emailed", saved.getId(), category, failure);
		}
	}

	/** Only a platform this project ships to; anything else is recorded as unknown. */
	private static String platformOf(String raw) {
		String cleaned = trimmedOrNull(raw);
		if (cleaned == null) {
			return null;
		}
		String upper = cleaned.toUpperCase(java.util.Locale.ROOT);
		return PLATFORMS.contains(upper) ? upper : null;
	}

	private static String trimmedOrNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.strip();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
