package ro.garajulmeu.email;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import ro.garajulmeu.user.Language;

/**
 * Writes every message to a file the end-to-end suite can read.
 *
 * <p><strong>This class lives in the test source tree on purpose.</strong> A
 * verification code is hashed in the database and never stored in the clear -
 * section 27 - so an end-to-end test cannot recover it by querying. The
 * alternatives were scraping the application log, which breaks the first time a
 * log format changes, or a test-only endpoint, which would be production
 * surface that must never exist. Living here means the class is absent from the
 * packaged jar: "production must not select this provider" is enforced by the
 * build rather than promised in a comment. And if the property were set in
 * production anyway, no provider would match and the application would refuse to
 * start, which is the failure mode {@link LoggingEmailProvider} already
 * documents.
 *
 * <p>Tab-separated rather than JSON, and that is a deliberate refusal of a
 * dependency. The fields are addresses, a language tag and six digits; none of
 * them can contain a tab, so there is nothing to escape and no serialiser
 * version to track. A reader in another language splits the line and is done.
 */
@Component
@ConditionalOnProperty(name = "garajul-meu.email.provider", havingValue = "recording")
public class RecordingEmailProvider implements EmailProvider {

	private static final Logger log = LoggerFactory.getLogger(RecordingEmailProvider.class);

	private static final String FILE_NAME = "messages.tsv";

	private final Path file;

	public RecordingEmailProvider(
			@Value("${garajul-meu.email.recording-directory:target/e2e-mail}") String directory) {

		this.file = Path.of(directory).resolve(FILE_NAME);

		try {
			Files.createDirectories(this.file.getParent());
			// Truncated at startup, so one run never reads another run's codes -
			// the suite looks for the *last* line matching an address, and a stale
			// file would let a passing test read a code that is no longer valid.
			Files.writeString(this.file, "", StandardCharsets.UTF_8);
		} catch (IOException exception) {
			throw new UncheckedIOException("Could not prepare the recorded mail file", exception);
		}

		log.warn("Email provider is RECORDING: messages are written to {} for the end-to-end "
				+ "suite and delivered nowhere. This provider is not in the production jar.", this.file);
	}

	@Override
	public void sendVerificationCode(String recipient, String code, Language language) {
		record("verification", recipient, "-", code, language);
	}

	@Override
	public void sendPasswordResetCode(String recipient, String code, Language language) {
		record("password-reset", recipient, "-", code, language);
	}

	@Override
	public void sendEmailChangeCode(String recipient, String newEmail, String code,
			Language language) {
		record("email-change", recipient, newEmail, code, language);
	}

	private synchronized void record(String purpose, String recipient, String requestedAddress,
			String code, Language language) {

		String line = String.join("\t", Instant.now().toString(), purpose, recipient,
				requestedAddress, language.code(), code) + System.lineSeparator();

		try {
			Files.writeString(file, line, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
		} catch (IOException exception) {
			throw new UncheckedIOException("Could not record an email for the end-to-end suite",
					exception);
		}
	}
}