package ro.garajulmeu;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Provides a throwaway PostgreSQL container for integration tests.
 *
 * <p>{@code @ServiceConnection} makes Spring Boot derive the datasource URL,
 * username and password from the running container, overriding any datasource
 * properties. Flyway then migrates that container, never a hosted database.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	/**
	 * Pinned to the same PostgreSQL version production runs, so tests exercise
	 * the dialect, types and behaviour the application meets there.
	 *
	 * <p><strong>17.6 since 2026-09-16, when production moved from Neon to
	 * Supabase</strong> and the version was read with {@code select version()} on
	 * the new project. Until then this was 18.4, Neon's version - a major version
	 * newer than production is now, which is the direction that lets a test pass
	 * on a feature production does not have. TRIGGER: Supabase upgrading the
	 * project's PostgreSQL, which it announces in the dashboard.
	 */
	private static final DockerImageName POSTGRES_IMAGE =
			DockerImageName.parse("postgres:17.6-alpine");

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(POSTGRES_IMAGE);
	}
}