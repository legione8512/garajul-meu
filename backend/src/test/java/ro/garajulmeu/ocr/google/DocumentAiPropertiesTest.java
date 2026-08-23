package ro.garajulmeu.ocr.google;

import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.util.Base64;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The client itself cannot be tested without an account and a paid page, so what
 * is tested is everything around it that can be got wrong silently: the resource
 * name, the region, a value nobody filled in, and the key.
 *
 * <p>The key is generated here rather than checked in, which is not squeamishness
 * - a committed private key in a public repository is a private key that has to
 * be rotated, even a useless one. It also means the test exercises the real
 * {@code service_account} path rather than a shape that merely parses.
 */
class DocumentAiPropertiesTest {

	private static final String SERVICE_ACCOUNT =
			"document-ai@garajul-meu-505722.iam.gserviceaccount.com";

	private static final DocumentAiProperties CONFIGURED =
			new DocumentAiProperties("garajul-meu-505722", "eu", "7f0ef1c4beff0c0f", null);

	@Test
	void namesTheProcessorTheWayTheApiExpects() {
		assertThat(CONFIGURED.processorName())
				.isEqualTo("projects/garajul-meu-505722/locations/eu/processors/7f0ef1c4beff0c0f");
	}

	/**
	 * The console displays the region as EU and the API wants eu. Copying what is
	 * on screen is the natural thing to do, and it answers NOT_FOUND with no hint
	 * as to why - so the difference is absorbed here rather than discovered at
	 * two in the morning.
	 */
	@Test
	void theRegionIsLowerCasedBecauseTheConsoleShowsItInCapitals() {
		DocumentAiProperties asShownInTheConsole =
				new DocumentAiProperties("garajul-meu-505722", "EU", "7f0ef1c4beff0c0f", null);

		assertThat(asShownInTheConsole.processorName()).isEqualTo(CONFIGURED.processorName());
		assertThat(asShownInTheConsole.endpoint()).isEqualTo("eu-documentai.googleapis.com:443");
	}

	@Test
	void aMissingValueIsRefusedAndSaysWhichPropertyItWas() {
		DocumentAiProperties incomplete =
				new DocumentAiProperties("garajul-meu-505722", "eu", "  ", null);

		assertThatThrownBy(incomplete::processorName)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("garajul-meu.ocr.google.processor-id");
	}

	/**
	 * The case that keeps a developer machine working. An absent key is not a
	 * misconfiguration to be caught - it is the instruction to let Google find
	 * the credentials `gcloud auth application-default login` left in the user
	 * profile. Blank counts as absent because the shipped configuration writes
	 * {@code ${GOOGLE_CREDENTIALS_JSON:}}, so an unset variable arrives as "".
	 */
	@Test
	void noKeyMeansApplicationDefaultCredentials() {
		assertThat(CONFIGURED.credentials()).isEmpty();

		assertThat(new DocumentAiProperties("p", "eu", "id", "   ").credentials())
				.as("an unset environment variable arrives as an empty string, not as null")
				.isEmpty();
	}

	@Test
	void aServiceAccountKeyIsReadAndScoped() throws Exception {
		DocumentAiProperties withKey =
				new DocumentAiProperties("garajul-meu-505722", "eu", "7f0ef1c4beff0c0f", serviceAccountKey());

		GoogleCredentials credentials = withKey.credentials().orElseThrow();

		assertThat(credentials).isInstanceOf(ServiceAccountCredentials.class);
		assertThat(((ServiceAccountCredentials) credentials).getClientEmail()).isEqualTo(SERVICE_ACCOUNT);

		assertThat(credentials.createScopedRequired())
				.as("scoped here, because a key read from JSON carries none and every "
						+ "call would be refused for a reason that blames authentication")
				.isFalse();
	}

	/**
	 * A malformed key must name the property and nothing else. The cause is
	 * attached deliberately - it is the only thing that says <em>how</em> the key
	 * was unreadable - but our own message is the one that reaches a log line, a
	 * startup banner and a Sentry title, so that is what is asserted on.
	 */
	@Test
	void anUnreadableKeyIsRefusedWithoutRepeatingIt() {
		DocumentAiProperties broken = new DocumentAiProperties(
				"garajul-meu-505722", "eu", "7f0ef1c4beff0c0f", "{\"private_key\":\"hunter2\"}");

		assertThatThrownBy(broken::credentials)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("garajul-meu.ocr.google.credentials-json")
				.hasMessageNotContaining("hunter2");
	}

	/** A real, throwaway RSA key in the shape Google's parser expects. */
	private static String serviceAccountKey() throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);

		String pem = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
				.encodeToString(generator.generateKeyPair().getPrivate().getEncoded());

		return """
				{
				  "type": "service_account",
				  "project_id": "garajul-meu-505722",
				  "private_key_id": "0123456789abcdef0123456789abcdef01234567",
				  "private_key": "-----BEGIN PRIVATE KEY-----\\n%s\\n-----END PRIVATE KEY-----\\n",
				  "client_email": "%s",
				  "client_id": "000000000000000000000",
				  "token_uri": "https://oauth2.googleapis.com/token"
				}
				""".formatted(pem.replace("\n", "\\n"), SERVICE_ACCOUNT);
	}
}