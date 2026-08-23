package ro.garajulmeu;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the two configuration files against edits whose consequences are
 * invisible until production.
 *
 * <p>Read as text and with no Spring context, for the same reason as {@link
 * ApplicationConfigurationTest}: src/test/resources/application.yml shadows the
 * shipped file, so a test that loaded a context would be asserting about the
 * wrong file entirely - and `application-prod.yml` is never loaded by any test
 * at all, which is exactly what makes it worth guarding this way. Nothing else
 * in the build reads it before Railway does.
 */
class ProductionConfigurationTest {

	private static final Path BASE = Path.of("src/main/resources/application.yml");

	private static final Path PRODUCTION = Path.of("src/main/resources/application-prod.yml");

	/** Comments are prose and may legitimately say "http://" or "always". */
	private static List<String> settingsOf(Path file) throws Exception {
		return Files.readAllLines(file).stream()
				.map(line -> line.replaceAll("#.*$", ""))
				.filter(line -> !line.isBlank())
				.toList();
	}

	@Test
	void theProductionProfileIsShippedAndNotJustDescribed() throws Exception {
		assertThat(PRODUCTION)
				.as("the prod profile Railway will activate with SPRING_PROFILES_ACTIVE")
				.exists();
	}

	/**
	 * <strong>The most dangerous single line either file could grow.</strong>
	 * SecurityConfig permits {@code /actuator/health} without a token and then
	 * falls through to {@code anyRequest().authenticated()}. There is no
	 * authorization rule on the chain - vehicle ownership is enforced in the
	 * repository query, so none has ever been needed - which means a second
	 * exposed endpoint is not admin-only. It is readable by everyone who has
	 * registered an account.
	 *
	 * <p>A wildcard would additionally expose {@code /actuator/env} and
	 * {@code /actuator/configprops}: the shape of every secret this application
	 * holds, the database host, and the whole provider configuration.
	 */
	@Test
	void actuatorExposesHealthAndNothingElse() throws Exception {
		Pattern exposure = Pattern.compile("^\\s*include:\\s*(.+?)\\s*$");

		for (Path file : List.of(BASE, PRODUCTION)) {
			List<String> exposed = settingsOf(file).stream()
					.map(exposure::matcher)
					.filter(java.util.regex.Matcher::matches)
					.map(matcher -> matcher.group(1))
					.toList();

			assertThat(exposed)
					.as("actuator exposure in %s - every entry is readable by any signed-in "
							+ "account, because no authorization rule stands between them", file)
					.allMatch("health"::equals);
		}
	}

	/**
	 * The health endpoint answers without a token, so it must answer with one
	 * word. Details name the database vendor, its host and the validation query;
	 * components list every registered indicator. Both are free reconnaissance
	 * for anybody who types the URL.
	 */
	@Test
	void theHealthEndpointNeverDescribesItself() throws Exception {
		for (Path file : List.of(BASE, PRODUCTION)) {
			assertThat(settingsOf(file))
					.as("health detail settings in %s", file)
					.noneMatch(line -> line.matches("^\\s*show-(details|components):\\s*(always|when[-_]authorized).*"));
		}
	}

	/**
	 * A localhost origin surviving into the production profile would be silent:
	 * the application starts, the real frontend is refused by CORS, and the only
	 * evidence is a browser console nobody is watching. The same edit in reverse
	 * - an http:// origin - would ask a Secure cookie to travel unencrypted,
	 * which it simply will not do.
	 */
	@Test
	void everyProductionOriginIsHttpsAndNoneIsLocal() throws Exception {
		List<String> settings = settingsOf(PRODUCTION);

		assertThat(settings)
				.as("origins in the production profile")
				.noneMatch(line -> line.contains("http://"))
				.noneMatch(line -> line.contains("localhost"))
				.noneMatch(line -> line.contains("127.0.0.1"));
	}
}