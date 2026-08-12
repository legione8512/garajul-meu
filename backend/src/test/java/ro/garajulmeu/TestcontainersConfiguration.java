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
	 * Pinned to the same PostgreSQL version Neon runs, so tests exercise the
	 * dialect, types and behaviour the application meets in production.
	 */
	private static final DockerImageName POSTGRES_IMAGE =
			DockerImageName.parse("postgres:18.4-alpine");

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(POSTGRES_IMAGE);
	}
}