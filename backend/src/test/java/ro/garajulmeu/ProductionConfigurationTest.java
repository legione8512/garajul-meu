package ro.garajulmeu;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * Guards the two configuration files against edits whose consequences are
 * invisible until production.
 *
 * <p>
 * With no Spring context, for the same reason as
 * {@link ApplicationConfigurationTest}: src/test/resources/application.yml
 * shadows the shipped file, so a test that loaded a context would be asserting
 * about the wrong file entirely - and `application-prod.yml` is never loaded by
 * any test at all, which is exactly what makes it worth guarding this way.
 * Nothing else in the build reads it before Railway does.
 *
 * <p>
 * <strong>Mostly as text, and in one place as parsed YAML.</strong> Reading
 * lines is right for asking whether a setting is present, and wrong for asking
 * what a setting *is* - see {@link #corsOriginsOf}.
 */
class ProductionConfigurationTest {

	private static final Path BASE = Path.of("src/main/resources/application.yml");

	private static final Path PRODUCTION = Path.of("src/main/resources/application-prod.yml");

	/** Comments are prose and may legitimately say "http://" or "always". */
	private static List<String> settingsOf(Path file) throws Exception {
		return Files.readAllLines(file).stream().map(line -> line.replaceAll("#.*$", ""))
				.filter(line -> !line.isBlank()).toList();
	}

	/**
	 * Parsed rather than read line by line, and the difference is why this method
	 * exists.
	 *
	 * <p>
	 * A text check for a line reading {@code - https://localhost} passes just as
	 * happily when that line has drifted under {@code ocr:}, or under
	 * {@code garajul-meu:} directly, or anywhere else two spaces of indentation can
	 * put it. Since nothing else in this build parses this file, a structural
	 * mistake would first be reported by Railway, at boot, in production - and the
	 * guard written to prevent exactly that would have stayed green.
	 *
	 * <p>
	 * SnakeYAML is already on the classpath: it is what Spring Boot itself uses to
	 * read these files, so this asks the question the way the application will.
	 */
	@SuppressWarnings("unchecked")
	private static List<String> corsOriginsOf(Path file) throws Exception {
		try (var in = Files.newInputStream(file)) {
			Map<String, Object> root = new Yaml().load(in);
			Map<String, Object> application = (Map<String, Object>) root.get("garajul-meu");
			Map<String, Object> cors = (Map<String, Object>) application.get("cors");

			return (List<String>) cors.get("allowed-origins");
		}
	}

	@Test
	void theProductionProfileIsShippedAndNotJustDescribed() throws Exception {
		assertThat(PRODUCTION).as("the prod profile Railway will activate with SPRING_PROFILES_ACTIVE").exists();
	}

	/**
	 * <strong>The most dangerous single line either file could grow.</strong>
	 * SecurityConfig permits {@code /actuator/health} without a token and then
	 * falls through to {@code anyRequest().authenticated()}. There is no
	 * authorization rule on the chain - vehicle ownership is enforced in the
	 * repository query, so none has ever been needed - which means a second exposed
	 * endpoint is not admin-only. It is readable by everyone who has registered an
	 * account.
	 *
	 * <p>
	 * A wildcard would additionally expose {@code /actuator/env} and
	 * {@code /actuator/configprops}: the shape of every secret this application
	 * holds, the database host, and the whole provider configuration.
	 */
	@Test
	void actuatorExposesHealthAndNothingElse() throws Exception {
		Pattern exposure = Pattern.compile("^\\s*include:\\s*(.+?)\\s*$");

		for (Path file : List.of(BASE, PRODUCTION)) {
			List<String> exposed = settingsOf(file).stream().map(exposure::matcher)
					.filter(java.util.regex.Matcher::matches).map(matcher -> matcher.group(1)).toList();

			assertThat(exposed)
					.as("actuator exposure in %s - every entry is readable by any signed-in "
							+ "account, because no authorization rule stands between them", file)
					.allMatch("health"::equals);
		}
	}

	/**
	 * The health endpoint answers without a token, so it must answer with one word.
	 * Details name the database vendor, its host and the validation query;
	 * components list every registered indicator. Both are free reconnaissance for
	 * anybody who types the URL.
	 */
	@Test
	void theHealthEndpointNeverDescribesItself() throws Exception {
		for (Path file : List.of(BASE, PRODUCTION)) {
			assertThat(settingsOf(file)).as("health detail settings in %s", file).noneMatch(
					line -> line.matches("^\\s*show-(details|components):\\s*(always|when[-_]authorized).*"));
		}
	}

	/**
	 * The gap this class was written to cover and did not: the file was guarded
	 * line by line and never parsed, so a tab character or a mis-indented block
	 * would have left every assertion here green and broken the deploy instead.
	 */
	@Test
	void theProductionProfileIsValidYamlWithTheOriginsWhereTheyBelong() throws Exception {
		assertThat(corsOriginsOf(PRODUCTION)).as("garajul-meu.cors.allowed-origins, parsed").isNotNull().isNotEmpty();
	}

	/**
	 * A localhost origin surviving into the production profile would be silent: the
	 * application starts, the real frontend is refused by CORS, and the only
	 * evidence is a browser console nobody is watching. The same edit in reverse -
	 * an http:// origin - would ask a Secure cookie to travel unencrypted, which it
	 * simply will not do.
	 *
	 * <p>
	 * <strong>One localhost origin is permitted from 2026-08-25, and the exception
	 * is narrow deliberately.</strong> A Capacitor WebView serves the bundle from
	 * its own local server, so the Android application's every call arrives with
	 * {@code Origin: https://localhost} and is refused without an entry for it.
	 * That entry is a decision; a leftover {@code http://localhost:5173} is a
	 * mistake, and a substring ban could not tell the two apart.
	 *
	 * <p>
	 * <strong>Widened on 2026-09-04, after the question it named was
	 * answered.</strong> This assertion used to require every origin to begin
	 * {@code https://}, which caught the phase-18 trap by accident: iOS sends
	 * {@code capacitor://localhost} and there is no configuration that changes it
	 * - the CLI states the iOS scheme "can't be set to schemes that the WKWebView
	 * already handles, such as http or https", so unlike Android it cannot be
	 * given {@code https}. The open question was whether Spring's
	 * CorsConfiguration accepts a non-http origin at all; {@code CorsTest} answers
	 * it through the real filter chain, and it does.
	 *
	 * <p>
	 * So the rule is now <em>two</em> exact WebView origins and https for
	 * everything else, rather than one exception and a scheme test. That is
	 * deliberately not a relaxation: naming both in full still refuses a third
	 * scheme, a third localhost spelling, and a port. What it will not do is
	 * quietly accept a {@code capacitor://} origin pointing anywhere but
	 * localhost, which is why the list is compared whole rather than filtered.
	 */
	@Test
	void everyProductionOriginIsHttpsExceptTheTwoNativeWebViews() throws Exception {
		List<String> origins = corsOriginsOf(PRODUCTION);

		assertThat(origins).as("origins in the production profile")
				.allMatch(origin -> origin.startsWith("https://") || origin.equals("capacitor://localhost"))
				.noneMatch(origin -> origin.contains("127.0.0.1"));

		assertThat(origins.stream().filter(origin -> origin.contains("localhost")).toList())
				.as("localhost origins in the production profile")
				.containsExactlyInAnyOrder("https://localhost", "capacitor://localhost");
	}
}