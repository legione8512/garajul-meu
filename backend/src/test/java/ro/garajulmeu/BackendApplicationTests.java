package ro.garajulmeu;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("""
		Requires a database since Phase 2 added Spring Data JPA. \
		Re-enabled in Phase 3 with Testcontainers PostgreSQL. \
		Specification section 20 forbids running automated tests \
		against the Neon development branch.""")
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}