package ro.garajulmeu.push.firebase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.google.auth.oauth2.GoogleCredentials;

/**
 * What may speak to Firebase Cloud Messaging on this project's behalf.
 *
 * <p>
 * One value, and it is a private key. There is no project identifier here
 * because there does not need to be: a service account key names its own
 * project, and the Firebase SDK reads it from the key rather than from
 * configuration. Adding one would create a second source of truth whose only
 * possible contribution is disagreeing with the first.
 *
 * <p>
 * Bound whatever the active provider is, so under {@code logging} - which is
 * the whole of V1 web - it is simply null and nothing reads it. <strong>The
 * check lives in the method rather than the constructor</strong> for exactly
 * that reason: a missing key must fail the firebase path loudly and leave the
 * logging path alone.
 */
@ConfigurationProperties(prefix = "garajul-meu.push.firebase")
public record FirebaseProperties(String credentialsJson) {

	/**
	 * The narrow scope, not the platform one. Document AI had to take
	 * {@code cloud-platform} because Google publishes nothing smaller for it;
	 * messaging publishes exactly this, so there is no excuse for asking for more.
	 * What actually bounds the account is its IAM role - {@code Firebase
	 * Cloud Messaging API Admin} and nothing else - and this is the second lock.
	 */
	private static final List<String> SCOPE = List.of("https://www.googleapis.com/auth/firebase.messaging");

	/**
	 * The service account key, when one is configured.
	 *
	 * <p>
	 * <strong>Empty is a supported answer, not a failure</strong>, and it means the
	 * same thing it means for Document AI: on a machine with {@code gcloud
	 * auth application-default login} the credentials are found without being
	 * handed over. Inside a container there is no such file, so the key has to
	 * arrive as a value; absent it, Google's own lookup fails and the application
	 * does not start, which is the loud failure this seam wants.
	 *
	 * <p>
	 * Scoped explicitly, because a key read from JSON arrives with none and every
	 * send would be refused for a reason that names authentication rather than
	 * scopes.
	 *
	 * <p>
	 * The exception carries the property name and nothing else. The value is a
	 * private key and has no business in a message, a log, or a Sentry event.
	 */
	public Optional<GoogleCredentials> credentials() {
		if (credentialsJson == null || credentialsJson.isBlank()) {
			return Optional.empty();
		}

		try (InputStream key = new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8))) {
			return Optional.of(GoogleCredentials.fromStream(key).createScoped(SCOPE));
		} catch (IOException unreadable) {
			throw new IllegalStateException(
					"garajul-meu.push.firebase.credentials-json is not a readable service account key", unreadable);
		}
	}

	/**
	 * What the SDK would find for itself, made explicit so both paths look alike.
	 */
	public static GoogleCredentials applicationDefault() {
		try {
			return GoogleCredentials.getApplicationDefault().createScoped(SCOPE);
		} catch (IOException missing) {
			throw new IllegalStateException("No Firebase credentials: set garajul-meu.push.firebase.credentials-json, "
					+ "or provide application default credentials", missing);
		}
	}
}