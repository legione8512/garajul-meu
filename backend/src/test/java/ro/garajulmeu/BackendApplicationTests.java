package ro.garajulmeu;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BackendApplicationTests {

	@Autowired
	private DataSource dataSource;

	@Test
	void contextLoads() {
	}

	/**
	 * Guards specification section 20: automated tests must never touch the
	 * hosted Neon database. If a future configuration change lets the Neon URL
	 * leak into the test context, this fails immediately and unambiguously.
	 */
	@Test
	void usesThrowawayContainerAndNeverTheHostedDatabase() throws SQLException {
		try (Connection connection = dataSource.getConnection()) {
			assertThat(connection.getMetaData().getURL())
					.contains("localhost")
					.doesNotContain("neon.tech");
		}
	}
}