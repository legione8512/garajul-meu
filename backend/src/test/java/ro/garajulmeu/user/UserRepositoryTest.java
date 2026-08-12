package ro.garajulmeu.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import ro.garajulmeu.TestcontainersConfiguration;

/**
 * {@code @Transactional} rolls each test back, so tests cannot see or collide
 * with one another's rows. {@code saveAndFlush} forces the INSERT to reach the
 * database inside the test, which is the only way a constraint violation can be
 * observed rather than surfacing later at commit.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class UserRepositoryTest {

	@Autowired
	private UserRepository userRepository;

	@Test
	void appliesTheDefaultsDeclaredInTheMigration() {
		User saved = userRepository.saveAndFlush(
				new User("Marius Robert", "defaults@example.com", "argon2-placeholder"));

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getPreferredLanguage()).isEqualTo(Language.RO);
		assertThat(saved.getTimezone()).isEqualTo("Europe/Bucharest");
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getUpdatedAt()).isNotNull();
	}

	@Test
	void leavesANewAccountUnverified() {
		User saved = userRepository.saveAndFlush(
				new User("Marius Robert", "unverified@example.com", "argon2-placeholder"));

		assertThat(saved.getEmailVerifiedAt()).isNull();
		assertThat(saved.isEmailVerified()).isFalse();
	}

	@Test
	void findsAnAccountByItsNormalisedEmail() {
		userRepository.saveAndFlush(new User("Marius Robert", "findme@example.com", "argon2-placeholder"));

		assertThat(userRepository.findByEmail("findme@example.com")).isPresent();
		assertThat(userRepository.findByEmail("absent@example.com")).isEmpty();
	}

	/** Section 9: the email is globally unique after normalisation. */
	@Test
	void refusesASecondAccountWithTheSameEmail() {
		userRepository.saveAndFlush(new User("First Owner", "shared@example.com", "argon2-placeholder"));

		assertThatThrownBy(() -> userRepository.saveAndFlush(
				new User("Second Owner", "shared@example.com", "argon2-placeholder")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void storesTheLanguageAsItsLowerCaseCode() {
		User user = userRepository.saveAndFlush(
				new User("Marius Robert", "english@example.com", "argon2-placeholder"));
		user.setPreferredLanguage(Language.EN);
		userRepository.flush();

		assertThat(userRepository.findByEmail("english@example.com"))
				.get()
				.extracting(User::getPreferredLanguage)
				.isEqualTo(Language.EN);
	}
}