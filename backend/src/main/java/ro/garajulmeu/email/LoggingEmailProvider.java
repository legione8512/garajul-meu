package ro.garajulmeu.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import ro.garajulmeu.user.Language;

/**
 * Writes the message to the log instead of sending it, so the whole registration
 * flow can be exercised without a Resend account or a verified domain.
 *
 * <p><strong>Development only.</strong> Specification section 30 forbids logging
 * verification codes; this class exists precisely because in development the log
 * <em>is</em> the inbox. It is selected by an explicit property, so production
 * cannot fall back to it silently - if the property is absent there is no
 * EmailProvider bean at all and the application refuses to start.
 */
@Component
@ConditionalOnProperty(name = "garajul-meu.email.provider", havingValue = "logging")
public class LoggingEmailProvider implements EmailProvider {

	private static final Logger log = LoggerFactory.getLogger(LoggingEmailProvider.class);

	public LoggingEmailProvider() {
		log.warn("Email provider is LOGGING: verification codes are written to this log, "
				+ "not delivered. This must never be the case in production.");
	}

	@Override
	public void sendVerificationCode(String recipient, String code, Language language) {
		write("verification code", recipient, code, language);
	}

	@Override
	public void sendPasswordResetCode(String recipient, String code, Language language) {
		write("password reset code", recipient, code, language);
	}

	private void write(String purpose, String recipient, String code, Language language) {
		log.info("""

				=====================================================
				 DEVELOPMENT EMAIL - {}
				 to:       {}
				 language: {}
				 code:     {}
				=====================================================
				""", purpose, recipient, language.code(), code);
	}
}