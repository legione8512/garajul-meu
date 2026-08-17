package ro.garajulmeu.notification;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import ro.garajulmeu.TestcontainersConfiguration;
import ro.garajulmeu.email.EmailProvider;
import ro.garajulmeu.security.AccessTokenService;
import ro.garajulmeu.user.User;
import ro.garajulmeu.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Annotations identical to AuthFlowTest, including the unused EmailProvider
 * mock, so the cached context is reused and no further container starts.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class NotificationPreferencesFlowTest {

	private static final String PATH = "/api/v1/users/me/notification-preferences";

	private static final String ALL_ON = """
			{"notificationsEnabled":true,"remind30Days":true,"remind14Days":true,
			 "remind7Days":true,"remind3Days":true,"remind1Day":true,
			 "remindOnExpiry":true,"notificationLocalTime":"09:00:00"}
			""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private NotificationPreferencesRepository preferencesRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AccessTokenService accessTokenService;

	/** Unused. Present only so this class shares AuthFlowTest's context. */
	@MockitoBean
	private EmailProvider emailProvider;

	private record Account(UUID id, String token) {
	}

	private Account givenAccount(String email) {
		User user = new User("Marius Robert", email, passwordEncoder.encode("a-long-enough-password"));
		user.setEmailVerifiedAt(Instant.now());
		UUID id = userRepository.saveAndFlush(user).getId();
		return new Account(id, accessTokenService.issueFor(id).value());
	}

	/**
	 * A read must not write. Somebody opening the settings screen and closing it
	 * again has said nothing, and the row that would have been created holds
	 * exactly what its absence already means.
	 */
	@Test
	void anAccountThatHasNeverSavedAnythingReadsTheDefaultsAndStaysUnwritten() throws Exception {
		Account account = givenAccount("defaults@example.com");

		mockMvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.notificationsEnabled").value(true))
				.andExpect(jsonPath("$.remind30Days").value(true))
				.andExpect(jsonPath("$.remindOnExpiry").value(true))
				.andExpect(jsonPath("$.notificationLocalTime").value("09:00:00"));

		assertThat(preferencesRepository.findByUserId(account.id())).isEmpty();
	}

	@Test
	void savingCreatesTheRowAndReadsBack() throws Exception {
		Account account = givenAccount("saving@example.com");

		mockMvc.perform(put(PATH)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"notificationsEnabled":true,"remind30Days":false,"remind14Days":true,
								 "remind7Days":false,"remind3Days":true,"remind1Day":false,
								 "remindOnExpiry":true,"notificationLocalTime":"18:30:00"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.remind30Days").value(false))
				.andExpect(jsonPath("$.notificationLocalTime").value("18:30:00"));

		NotificationPreferences stored =
				preferencesRepository.findByUserId(account.id()).orElseThrow();
		assertThat(stored.isRemind30Days()).isFalse();
		assertThat(stored.isRemind14Days()).isTrue();
		assertThat(stored.getNotificationLocalTime()).isEqualTo(LocalTime.of(18, 30));
	}

	/** Saving twice replaces rather than adding a second row: section 10.5 says one-to-one. */
	@Test
	void savingTwiceKeepsOneRow() throws Exception {
		Account account = givenAccount("twice@example.com");

		for (int i = 0; i < 2; i++) {
			mockMvc.perform(put(PATH)
							.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token())
							.contentType(MediaType.APPLICATION_JSON)
							.content(ALL_ON))
					.andExpect(status().isOk());
		}

		assertThat(preferencesRepository.count()).isEqualTo(1);
	}

	/**
	 * The one failure mode a preferences screen must not have. A primitive boolean
	 * would read a forgotten field as false and quietly turn that reminder off;
	 * boxed and @NotNull, it is refused instead.
	 */
	@Test
	void aBodyMissingOneSwitchIsRefusedRatherThanReadAsOff() throws Exception {
		Account account = givenAccount("partial@example.com");

		mockMvc.perform(put(PATH)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"notificationsEnabled":true,"remind30Days":true,"remind14Days":true,
								 "remind7Days":true,"remind3Days":true,"remind1Day":true,
								 "notificationLocalTime":"09:00:00"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		assertThat(preferencesRepository.findByUserId(account.id())).isEmpty();
	}

	@Test
	void oneAccountCannotSeeOrChangeAnothersPreferences() throws Exception {
		Account mine = givenAccount("mine-prefs@example.com");
		Account theirs = givenAccount("theirs-prefs@example.com");

		mockMvc.perform(put(PATH)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + theirs.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"notificationsEnabled":false,"remind30Days":false,"remind14Days":false,
								 "remind7Days":false,"remind3Days":false,"remind1Day":false,
								 "remindOnExpiry":false,"notificationLocalTime":"07:00:00"}
								"""))
				.andExpect(status().isOk());

		mockMvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + mine.token()))
				.andExpect(jsonPath("$.notificationsEnabled").value(true))
				.andExpect(jsonPath("$.notificationLocalTime").value("09:00:00"));
	}

	/** Section 24: deleting an account takes what it knew about the person with it. */
	@Test
	void deletingTheAccountTakesItsPreferencesWithIt() throws Exception {
		Account account = givenAccount("cascade-prefs@example.com");

		mockMvc.perform(put(PATH)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(ALL_ON))
				.andExpect(status().isOk());

		userRepository.deleteById(account.id());
		userRepository.flush();

		assertThat(preferencesRepository.findByUserId(account.id())).isEmpty();
	}

	/**
	 * The migration's column defaults and the entity's field initialisers answer
	 * for different situations - a row the database inserts, and a set of
	 * preferences for an account that has none - and they have to agree. This is
	 * what says so.
	 */
	@Test
	void theStoredDefaultsAndTheUnsavedDefaultsAgree() {
		Account account = givenAccount("agree@example.com");

		NotificationPreferences fresh = preferencesRepository.saveAndFlush(
				new NotificationPreferences(account.id()));
		NotificationPreferences unsaved = NotificationPreferences.defaultsFor(account.id());

		assertThat(fresh.isNotificationsEnabled()).isEqualTo(unsaved.isNotificationsEnabled());
		assertThat(fresh.isRemind30Days()).isEqualTo(unsaved.isRemind30Days());
		assertThat(fresh.isRemindOnExpiry()).isEqualTo(unsaved.isRemindOnExpiry());
		assertThat(fresh.getNotificationLocalTime()).isEqualTo(unsaved.getNotificationLocalTime());
	}

	@Test
	void bothEndpointsRefuseACallerWithNoToken() throws Exception {
		mockMvc.perform(get(PATH)).andExpect(status().isUnauthorized());
		mockMvc.perform(put(PATH).contentType(MediaType.APPLICATION_JSON).content(ALL_ON))
				.andExpect(status().isUnauthorized());
	}
}