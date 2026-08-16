package ro.garajulmeu;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reads the shipped configuration file as text, deliberately.
 *
 * <p>The whole {@code ocr:} block spent two days one indent level too far left,
 * a sibling of {@code garajul-meu:} rather than a child. Nothing complained:
 * OcrProperties substitutes a default for every zero, so a block that never
 * bound looked exactly like one that bound correctly, and every value happened
 * to match. It surfaced only when a bean finally needed the property through
 * {@code @ConditionalOnProperty}, which reads the environment and cannot be
 * helped by any default - and by then the application refused to start.
 *
 * <p>No Spring context here on purpose. Loading one would read the test
 * configuration, which shadows the file this is about.
 */
class ApplicationConfigurationTest {

	private static final Path SHIPPED = Path.of("src/main/resources/application.yml");

	/** A key at column zero, which in this file means a root. */
	private static final Pattern ROOT_KEY = Pattern.compile("^([a-z][a-z0-9-]*):.*");

	/** Two belong to Spring; the third is every setting this application owns. */
	private static final Set<String> ROOTS = Set.of("spring", "logging", "garajul-meu");

	@Test
	void everySettingSitsUnderARootSpringOrThisApplicationRecognises() throws Exception {
		List<String> roots = Files.readAllLines(SHIPPED).stream()
				.map(ROOT_KEY::matcher)
				.filter(Matcher::matches)
				.map(matcher -> matcher.group(1))
				.toList();

		assertThat(roots)
				.as("a root this file does not expect is almost always a block that lost an indent")
				.isNotEmpty()
				.isSubsetOf(ROOTS);
	}
}