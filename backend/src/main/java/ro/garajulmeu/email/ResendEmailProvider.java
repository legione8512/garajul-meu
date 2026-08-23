package ro.garajulmeu.email;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import ro.garajulmeu.user.Language;

/**
 * Delivers transactional email through Resend. Specification section 32's seam,
 * named there as {@code EmailProvider → ResendEmailProvider}.
 *
 * <p><strong>Nothing here logs a code, an address or a message body.</strong>
 * Section 27 forbids verification codes reaching a log, and the address is a
 * personal field the log has no use for. What is logged instead is the purpose
 * and the identifier Resend returns, which is exactly what somebody chasing a
 * report of "the email never arrived" needs and contains nothing about who it
 * was for.
 *
 * <p>A send failure is deliberately not caught. Registration sends inside its
 * transaction, so a provider outage rolls the account back rather than leaving
 * somebody with an account and no way to verify it - and the exception reaches
 * {@code GlobalExceptionHandler}, which logs it at ERROR, which is what Sentry
 * reports. An email provider that has stopped answering is precisely the sort of
 * thing worth being told about at once.
 */
@Component
@ConditionalOnProperty(name = "garajul-meu.email.provider", havingValue = "resend")
public class ResendEmailProvider implements EmailProvider {

	private static final Logger log = LoggerFactory.getLogger(ResendEmailProvider.class);

	/** Resend's documented request shape: from, to, subject, and text or html. */
	private record Payload(String from, List<String> to, String subject, String text) {
	}

	private record Sent(String id) {
	}

	private final RestClient client;

	private final EmailMessages messages;

	private final String from;

	/**
	 * Spring Boot on this classpath auto-configures no {@code RestClient.Builder}
	 * - checked against the jars rather than assumed - so the client is built
	 * here instead of injected.
	 */
	@Autowired
	ResendEmailProvider(EmailProperties properties, EmailMessages messages) {
		this(properties, messages, RestClient.builder());
	}

	/** Visible for the test, which binds a MockRestServiceServer to the builder. */
	ResendEmailProvider(EmailProperties properties, EmailMessages messages, RestClient.Builder builder) {
		this.from = required(properties.from(), "garajul-meu.email.from");
		this.messages = messages;
		this.client = builder
				.baseUrl(properties.baseUrl())
				.defaultHeader(HttpHeaders.AUTHORIZATION,
						"Bearer " + required(properties.apiKey(), "garajul-meu.email.api-key"))
				.build();

		log.info("Email provider is RESEND, sending as {}", from);
	}

	/**
	 * Fails at startup rather than at the first registration. An empty string is
	 * treated as missing on purpose: the shipped configuration writes
	 * {@code ${RESEND_API_KEY:}}, so an unset variable arrives here as "" and
	 * would otherwise produce a request Resend refuses, hours later, for a reason
	 * nobody would connect to a missing environment variable.
	 */
	private static String required(String value, String property) {
		if (value == null || value.isBlank()) {
			throw new IllegalStateException(
					property + " must be set when the email provider is \"resend\"");
		}
		return value;
	}

	@Override
	public void sendVerificationCode(String recipient, String code, Language language) {
		send("verification code", recipient, messages.verification(code, language));
	}

	@Override
	public void sendPasswordResetCode(String recipient, String code, Language language) {
		send("password reset code", recipient, messages.passwordReset(code, language));
	}

	@Override
	public void sendEmailChangeCode(String recipient, String newEmail, String code, Language language) {
		send("email change code", recipient, messages.emailChange(newEmail, code, language));
	}

	private void send(String purpose, String recipient, EmailMessages.Message message) {
		Sent sent = client.post()
				.uri("/emails")
				.contentType(MediaType.APPLICATION_JSON)
				.body(new Payload(from, List.of(recipient), message.subject(), message.body()))
				.retrieve()
				.body(Sent.class);

		log.info("Sent a {} through Resend as message {}", purpose, sent == null ? "unknown" : sent.id());
	}
}