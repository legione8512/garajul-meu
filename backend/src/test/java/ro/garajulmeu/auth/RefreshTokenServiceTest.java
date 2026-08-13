package ro.garajulmeu.auth;

import java.time.Instant;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class RefreshTokenServiceTest {

	@Autowired
	private RefreshTokenService refreshTokenService;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private UserRepository userRepository;

	private UUID givenUser(String email) {
		return userRepository.saveAndFlush(new User("Marius Robert", email, "argon2-placeholder")).getId();
	}

	private static ErrorCode codeOf(Throwable thrown) {
		return ((ApiException) thrown).errorCode();
	}

	@Test
	void storesOnlyTheDigestNeverTheTokenItself() {
		IssuedRefreshToken issued = refreshTokenService.startFamily(givenUser("digest@example.com"));

		RefreshToken stored = refreshTokenRepository.findById(issued.id()).orElseThrow();

		assertThat(stored.getTokenHash())
				.isNotEqualTo(issued.value())
				.isEqualTo(RefreshTokenService.sha256(issued.value()))
				.hasSize(64);
	}

	@Test
	void rotationStaysInTheSameFamilyAndLinksTheChain() {
		IssuedRefreshToken first = refreshTokenService.startFamily(givenUser("rotate@example.com"));

		IssuedRefreshToken second = refreshTokenService.rotate(first.value());

		assertThat(second.familyId()).isEqualTo(first.familyId());
		assertThat(second.value()).isNotEqualTo(first.value());

		RefreshToken spent = refreshTokenRepository.findById(first.id()).orElseThrow();
		assertThat(spent.getRevokedAt()).isNotNull();
		assertThat(spent.getReplacedByTokenId()).isEqualTo(second.id());
	}

	/** The mechanism that turns a stolen token into an alarm instead of access. */
	@Test
	void replayingASpentTokenRevokesTheWholeFamily() {
		IssuedRefreshToken first = refreshTokenService.startFamily(givenUser("replay@example.com"));
		IssuedRefreshToken second = refreshTokenService.rotate(first.value());

		assertThatThrownBy(() -> refreshTokenService.rotate(first.value()))
				.isInstanceOf(ApiException.class)
				.extracting(RefreshTokenServiceTest::codeOf)
				.isEqualTo(ErrorCode.REFRESH_TOKEN_REUSED);

		// The honest holder's current token is collateral damage, on purpose.
		assertThat(refreshTokenRepository.findById(second.id()).orElseThrow().getRevokedAt()).isNotNull();

		assertThatThrownBy(() -> refreshTokenService.rotate(second.value()))
				.isInstanceOf(ApiException.class)
				.extracting(RefreshTokenServiceTest::codeOf)
				.isEqualTo(ErrorCode.REFRESH_TOKEN_REUSED);
	}

	@Test
	void refusesATokenThatWasNeverIssued() {
		assertThatThrownBy(() -> refreshTokenService.rotate("not-a-token-we-ever-issued"))
				.isInstanceOf(ApiException.class)
				.extracting(RefreshTokenServiceTest::codeOf)
				.isEqualTo(ErrorCode.REFRESH_TOKEN_INVALID);
	}

	@Test
	void refusesAnExpiredToken() {
		UUID userId = givenUser("expired-refresh@example.com");
		String raw = "a-token-value-that-we-control";
		refreshTokenRepository.saveAndFlush(new RefreshToken(
				userId, RefreshTokenService.sha256(raw), UUID.randomUUID(), Instant.now().minusSeconds(60)));

		assertThatThrownBy(() -> refreshTokenService.rotate(raw))
				.isInstanceOf(ApiException.class)
				.extracting(RefreshTokenServiceTest::codeOf)
				.isEqualTo(ErrorCode.REFRESH_TOKEN_INVALID);
	}

	@Test
	void logoutEndsTheSessionForEveryTokenInTheFamily() {
		IssuedRefreshToken first = refreshTokenService.startFamily(givenUser("logout@example.com"));
		IssuedRefreshToken second = refreshTokenService.rotate(first.value());

		refreshTokenService.revokeSessionOf(second.value());

		assertThatThrownBy(() -> refreshTokenService.rotate(second.value()))
				.isInstanceOf(ApiException.class)
				.extracting(RefreshTokenServiceTest::codeOf)
				.isEqualTo(ErrorCode.REFRESH_TOKEN_REUSED);
	}

	@Test
	void twoLoginsOnDifferentDevicesGetIndependentFamilies() {
		UUID userId = givenUser("devices@example.com");

		IssuedRefreshToken phone = refreshTokenService.startFamily(userId);
		IssuedRefreshToken laptop = refreshTokenService.startFamily(userId);

		assertThat(phone.familyId()).isNotEqualTo(laptop.familyId());

		refreshTokenService.revokeSessionOf(phone.value());

		// Logging out on the phone must not sign the laptop out.
		assertThat(refreshTokenService.rotate(laptop.value())).isNotNull();
	}
}