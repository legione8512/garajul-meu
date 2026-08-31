package ro.garajulmeu.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import ro.garajulmeu.TestcontainersConfiguration;
import ro.garajulmeu.auth.RefreshTokenService.IssuedRefreshToken;
import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;
import ro.garajulmeu.user.User;
import ro.garajulmeu.user.UserRepository;

/**
 * That the grace window is a window, and not merely a condition on the
 * successor.
 *
 * <p>
 * Its own context, with the window closed to nothing. The alternative was
 * writing {@code revokedAt} into the past by hand, which would need a setter
 * that exists for no other reason and would let a test reach past the entity's
 * own vocabulary. A property is what the window is; setting it is the honest
 * way to ask what happens outside it.
 *
 * <p>
 * Every other case lives in RefreshTokenServiceTest, on the real default - this
 * class exists to vary the one thing that class cannot.
 */
@SpringBootTest(properties = "garajul-meu.auth.refresh-reuse-grace=0s")
@Import(TestcontainersConfiguration.class)
@Transactional
class RefreshTokenGraceWindowTest {

	@Autowired
	private RefreshTokenService refreshTokenService;

	@Autowired
	private UserRepository userRepository;

	private UUID givenUser(String email) {
		return userRepository.saveAndFlush(new User("Marius Robert", email, "argon2-placeholder")).getId();
	}

	@Test
	void refusesAReplayOnceTheWindowHasClosed() {
		IssuedRefreshToken first = refreshTokenService.startFamily(givenUser("window@example.com"));
		refreshTokenService.rotate(first.value());

		// The successor is untouched and the rotation was a moment ago: the only
		// thing refusing this is the window itself.
		assertThatThrownBy(() -> refreshTokenService.rotate(first.value())).isInstanceOf(ApiException.class)
				.extracting(thrown -> ((ApiException) thrown).errorCode()).isEqualTo(ErrorCode.REFRESH_TOKEN_REUSED);
	}
}