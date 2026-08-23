package ro.garajulmeu.ocr.google;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.documentai.v1.ProcessorName;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Which processor to call, and what may call it.
 *
 * <p>The first three identifiers are <strong>not</strong> secret: they name a
 * resource, they do not open it, which is why they sit in a file this repository
 * publishes. {@code credentialsJson} is the opposite of all three - it is a
 * private key - and it has no value in any committed file, only the empty
 * default of an environment variable.
 *
 * <p>Bound whatever the active provider is, so with the stub they are simply
 * null and nothing reads them. <strong>The checks live in the methods rather
 * than in the constructor</strong> for exactly that reason: a missing value must
 * fail the google path loudly and leave the stub path alone.
 */
@ConfigurationProperties(prefix = "garajul-meu.ocr.google")
public record DocumentAiProperties(
		String projectId, String location, String processorId, String credentialsJson) {

	/**
	 * Document AI publishes no narrower scope than the platform one, so this is
	 * the least that works rather than a convenient maximum. What actually bounds
	 * the account is its IAM role, which is {@code Document AI API User} and
	 * nothing else.
	 */
	private static final List<String> SCOPE =
			List.of("https://www.googleapis.com/auth/cloud-platform");

	/**
	 * The regional endpoint, and it is not optional. A processor created in the
	 * EU is invisible to the global endpoint: the call answers NOT_FOUND, which
	 * reads as "no such processor" when the truth is "you asked the wrong
	 * building".
	 */
	String endpoint() {
		return normalisedLocation() + "-documentai.googleapis.com:443";
	}

	String processorName() {
		return ProcessorName.of(
				required(projectId, "project-id"),
				normalisedLocation(),
				required(processorId, "processor-id")).toString();
	}

	/**
	 * The service account key, when one is configured.
	 *
	 * <p><strong>Empty is a supported answer, not a failure.</strong> On a
	 * developer machine the credentials come from {@code gcloud auth
	 * application-default login} and live in the user profile, so handing the
	 * client nothing is exactly right - it finds them itself. Inside a container
	 * there is no such file and no metadata server, so the key has to arrive as a
	 * value; absent it, Google's own lookup fails and the application does not
	 * start, which is the loud failure this seam wants.
	 *
	 * <p>Scoped explicitly, because a key read from JSON arrives with none and
	 * every call would be refused for a reason that names authentication rather
	 * than scopes.
	 *
	 * <p>The exception deliberately carries only the property name. The value is
	 * a private key and has no business in a message, a log or a Sentry event.
	 * The cause is attached because it is the only thing that says <em>how</em>
	 * the key was unreadable - we do not control its wording, which is why the
	 * key itself never reaches ours.
	 */
	Optional<GoogleCredentials> credentials() {
		if (credentialsJson == null || credentialsJson.isBlank()) {
			return Optional.empty();
		}

		try (InputStream key =
				new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8))) {
			return Optional.of(GoogleCredentials.fromStream(key).createScoped(SCOPE));
		}
		catch (IOException unreadable) {
			throw new IllegalStateException(
					"garajul-meu.ocr.google.credentials-json is not a readable service account key",
					unreadable);
		}
	}

	/**
	 * The console shows the region as {@code EU} and the API wants {@code eu}.
	 * Copying what is on screen is the obvious thing to do and produces a
	 * NOT_FOUND with no hint as to why, so it is normalised here rather than
	 * trusted to whoever fills in the file.
	 */
	private String normalisedLocation() {
		return required(location, "location").toLowerCase(Locale.ROOT);
	}

	private static String required(String value, String property) {
		if (value == null || value.isBlank()) {
			throw new IllegalStateException(
					"garajul-meu.ocr.google." + property + " must be set when the OCR provider is google");
		}
		return value;
	}
}