package ro.garajulmeu.push;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

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
import ro.garajulmeu.common.Sha256Hex;
import ro.garajulmeu.email.EmailProvider;
import ro.garajulmeu.security.AccessTokenService;
import ro.garajulmeu.user.User;
import ro.garajulmeu.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
class UserDeviceFlowTest {

	private static final String PATH = "/api/v1/devices";
	private static final String TOKEN = "fcm-token-aaaaaaaaaaaaaaaaaaaaaaaa-111111";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserDeviceRepository deviceRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AccessTokenService accessTokenService;

	/** For reading the column as the database holds it, past the converter. */
	@PersistenceContext
	private EntityManager entityManager;

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

	private String body(String pushToken, String platform) {
		return body(pushToken, platform, true);
	}

	/**
	 * {@code notificationsEnabled} is what the operating system currently permits,
	 * reported by the client at every launch. The two-argument form says "granted"
	 * because that is the ordinary case; a test about a revoked permission says so
	 * where it is read.
	 */
	private String body(String pushToken, String platform, boolean notificationsEnabled) {
		return """
				{"platform":"%s","pushToken":"%s","deviceName":"Telefonul lui Marius",\
				"notificationsEnabled":%s}
				""".formatted(platform, pushToken, notificationsEnabled);
	}

	/** The token's blind index, which is how a row is found now that the column is ciphertext. */
	private static String hashOf(String pushToken) {
		return Sha256Hex.of(pushToken);
	}

	@Test
	void registeringStoresTheDeviceAndAnswersWithoutTheToken() throws Exception {
		Account account = givenAccount("device@example.com");

		mockMvc.perform(post(PATH)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body(TOKEN, "ANDROID")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.platform").value("ANDROID"))
				.andExpect(jsonPath("$.deviceName").value("Telefonul lui Marius"))
				.andExpect(jsonPath("$.pushToken").doesNotExist())
				// Not only "the field is absent": the value must not appear anywhere
				// in the body, under any name a refactoring might give it.
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString(TOKEN))));

		assertThat(deviceRepository.findByPushTokenHash(hashOf(TOKEN))).isPresent();
	}

	/**
	 * <strong>Section 10.7, asserted on the column rather than on the cipher.</strong>
	 *
	 * <p>PushTokenCipherTest proves the encryption works. This proves it is
	 * actually applied - which is a different claim, and the one that would fail
	 * silently: remove {@code @Convert} from the entity and every other test in
	 * this file still passes, because the application would go on reading back
	 * exactly what it wrote. Only a query that bypasses the converter can tell the
	 * difference between an encrypted column and a plain one.
	 *
	 * <p>The token still has to come back out. Section 10.7 is explicit that the
	 * value must stay retrievable rather than hashed, because FCM needs it to
	 * send, so both halves are asserted here.
	 */
	@Test
	void theColumnHoldsCiphertextAndTheApplicationStillReadsTheToken() {
		Account account = givenAccount("encrypted-column@example.com");

		UserDevice saved = deviceRepository.saveAndFlush(
				new UserDevice(account.id(), DevicePlatform.ANDROID, TOKEN));

		String asStored = (String) entityManager
				.createNativeQuery("select push_token from user_devices where id = :id")
				.setParameter("id", saved.getId())
				.getSingleResult();

		assertThat(asStored)
				.as("what the database actually holds")
				.isNotEqualTo(TOKEN)
				.doesNotContain(TOKEN);

		assertThat(deviceRepository.findByPushTokenHash(hashOf(TOKEN)).orElseThrow().getPushToken())
				.as("and what the application reads back, because FCM needs the real value")
				.isEqualTo(TOKEN);
	}

	/** The native client calls this at every launch; twice must not mean two devices. */
	@Test
	void registeringTheSameTokenTwiceKeepsOneDevice() throws Exception {
		Account account = givenAccount("twice-device@example.com");

		for (int i = 0; i < 2; i++) {
			mockMvc.perform(post(PATH)
							.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token())
							.contentType(MediaType.APPLICATION_JSON)
							.content(body(TOKEN, "IOS")))
					.andExpect(status().isOk());
		}

		assertThat(deviceRepository.count()).isEqualTo(1);
	}

	/**
	 * The decision this table's unique index encodes. A token identifies an
	 * installation rather than an account: somebody signing in on a handset its
	 * previous owner used must take the registration with them, or that phone goes
	 * on receiving reminders about a garage it no longer has anything to do with.
	 */
	@Test
	void aTokenRegisteredByAnotherAccountMovesRatherThanBeingRefused() throws Exception {
		Account first = givenAccount("first-phone@example.com");
		Account second = givenAccount("second-phone@example.com");

		mockMvc.perform(post(PATH)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + first.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body(TOKEN, "ANDROID")))
				.andExpect(status().isOk());

		mockMvc.perform(post(PATH)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + second.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body(TOKEN, "ANDROID")))
				.andExpect(status().isOk());

		assertThat(deviceRepository.count()).isEqualTo(1);
		assertThat(deviceRepository.findByPushTokenHash(hashOf(TOKEN)).orElseThrow().getUserId())
				.isEqualTo(second.id());
	}

	/**
	 * A phone that cannot show a notification is registered all the same, and
	 * silenced.
	 *
	 * <p>Keeping the row rather than refusing it is what lets the next launch say
	 * the permission came back. Deleting on refusal would mean the account has no
	 * device at all, which reads identically to never having installed the
	 * application - and would throw away the name and the history for a state the
	 * person may undo in the operating system's settings a minute later.
	 */
	@Test
	void aDeviceThatCannotShowNotificationsIsStoredSilencedRatherThanRefused() throws Exception {
		Account account = givenAccount("denied-permission@example.com");

		mockMvc.perform(post(PATH)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body(TOKEN, "ANDROID", false)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.notificationsEnabled").value(false));

		assertThat(deviceRepository.findByPushTokenHash(hashOf(TOKEN)).orElseThrow()
				.isNotificationsEnabled()).isFalse();
	}

	/**
	 * The failure this whole field exists to close, run in the direction that
	 * actually bites.
	 *
	 * <p>A permission revoked in Android's settings months after registration
	 * leaves the token perfectly valid, so FCM would accept every message and the
	 * dispatcher would go on recording reminders as SENT while the person sees
	 * nothing. The only thing that can tell us is the client, at its next launch -
	 * and the launch is a plain re-registration, which is why the flag had to live
	 * on this endpoint rather than on one of its own.
	 */
	@Test
	void aLaterLaunchReportingARevokedPermissionSilencesAnAlreadyRegisteredDevice() throws Exception {
		Account account = givenAccount("revoked-later@example.com");

		mockMvc.perform(post(PATH)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body(TOKEN, "ANDROID", true)))
				.andExpect(status().isOk());

		mockMvc.perform(post(PATH)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body(TOKEN, "ANDROID", false)))
				.andExpect(status().isOk());

		assertThat(deviceRepository.count()).isEqualTo(1);
		assertThat(deviceRepository.findByUserIdAndNotificationsEnabledTrue(account.id()))
				.as("devices the dispatcher would still try to reach")
				.isEmpty();
	}

	/** And back again, because a permission granted afresh must be believed too. */
	@Test
	void aPermissionGrantedAgainMakesTheDeviceReachable() throws Exception {
		Account account = givenAccount("granted-again@example.com");

		mockMvc.perform(post(PATH)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body(TOKEN, "ANDROID", false)))
				.andExpect(status().isOk());

		mockMvc.perform(post(PATH)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body(TOKEN, "ANDROID", true)))
				.andExpect(status().isOk());

		assertThat(deviceRepository.findByUserIdAndNotificationsEnabledTrue(account.id()))
				.singleElement()
				.matches(device -> device.isNotificationsEnabled());
	}

	/**
	 * Required rather than defaulted, and this is the assertion that keeps it so.
	 * An optional field falling back to true would report every forgetful client's
	 * phone as reachable, which is the silent wrong answer the field was added to
	 * prevent.
	 */
	@Test
	void aRegistrationThatDoesNotSayWhetherItCanShowNotificationsIsRefused() throws Exception {
		Account account = givenAccount("silent-about-it@example.com");

		mockMvc.perform(post(PATH)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"platform":"ANDROID","pushToken":"%s","deviceName":"Telefon"}
								""".formatted(TOKEN)))
				.andExpect(status().isBadRequest());

		assertThat(deviceRepository.findByPushTokenHash(hashOf(TOKEN))).isEmpty();
	}

	@Test
	void unregisteringRemovesIt() throws Exception {
		Account account = givenAccount("unregister@example.com");

		UserDevice device = deviceRepository.saveAndFlush(
				new UserDevice(account.id(), DevicePlatform.IOS, TOKEN));

		mockMvc.perform(delete(PATH + "/" + device.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token()))
				.andExpect(status().isNoContent());

		assertThat(deviceRepository.findById(device.getId())).isEmpty();
	}

	/** Section 15: another account's device answers as one that does not exist. */
	@Test
	void anotherAccountsDeviceIsNotFoundAndSurvives() throws Exception {
		Account owner = givenAccount("owner-device@example.com");
		Account stranger = givenAccount("stranger-device@example.com");

		UserDevice device = deviceRepository.saveAndFlush(
				new UserDevice(owner.id(), DevicePlatform.IOS, TOKEN));

		mockMvc.perform(delete(PATH + "/" + device.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + stranger.token()))
				.andExpect(status().isNotFound());

		assertThat(deviceRepository.findById(device.getId())).isPresent();
	}

	/**
	 * Section 18: no web registrations in V1. The enum has two constants, so a
	 * third is refused by the deserialiser before anything of ours runs - which is
	 * the whole argument for taking the enum here rather than text.
	 */
	@Test
	void aPlatformOutsideTheTwoIsRefused() throws Exception {
		Account account = givenAccount("web-device@example.com");

		mockMvc.perform(post(PATH)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body(TOKEN, "WEB")))
				.andExpect(status().isBadRequest());

		assertThat(deviceRepository.count()).isZero();
	}

	/**
	 * Section 27 forbids logging full push tokens, so the entity refuses to print
	 * one even when something logs it by accident - which a default toString on an
	 * entity is exactly how it happens.
	 */
	@Test
	void theEntityRefusesToPrintItsToken() {
		UserDevice device = new UserDevice(UUID.randomUUID(), DevicePlatform.ANDROID, TOKEN);

		assertThat(device.toString()).doesNotContain(TOKEN);
		assertThat(device.fingerprint()).doesNotContain(TOKEN).hasSizeLessThan(TOKEN.length());
	}

	/** Section 24: deleting the account takes its registrations with it. */
	@Test
	void deletingTheAccountTakesItsDevicesWithIt() throws Exception {
		Account account = givenAccount("cascade-device@example.com");

		deviceRepository.saveAndFlush(new UserDevice(account.id(), DevicePlatform.IOS, TOKEN));

		userRepository.deleteById(account.id());
		userRepository.flush();

		assertThat(deviceRepository.count()).isZero();
	}

	@Test
	void bothEndpointsRefuseACallerWithNoToken() throws Exception {
		mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON)
						.content(body(TOKEN, "IOS")))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(delete(PATH + "/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
	}
}