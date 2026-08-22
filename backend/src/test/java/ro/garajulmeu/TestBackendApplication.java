package ro.garajulmeu;

import org.springframework.boot.SpringApplication;

/**
 * The application, started against a throwaway PostgreSQL container instead of a
 * hosted database. Run with {@code mvnw spring-boot:test-run}.
 *
 * <p>Specification section 26 is explicit that automated tests use Testcontainers
 * and never Neon development, and the end-to-end suite is an automated test. This
 * entry point is what lets Playwright drive a real backend without pointing it at
 * a database somebody's walkthrough data lives in - each run gets an empty schema
 * that Flyway migrates from nothing, which is also what makes the journey
 * repeatable: registering the same address twice would otherwise fail the second
 * time.
 *
 * <p>The {@code e2e} profile selects the recording email provider. Nothing else
 * in the test tree activates that profile, so the ordinary Spring tests are
 * untouched by it.
 */
public final class TestBackendApplication {

	private TestBackendApplication() {
	}

	public static void main(String[] args) {
		SpringApplication.from(BackendApplication::main)
				.with(TestcontainersConfiguration.class)
				.withAdditionalProfiles("e2e")
				.run(args);
	}
}