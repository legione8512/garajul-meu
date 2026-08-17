package ro.garajulmeu.push;

import java.time.Instant;
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
		return """
				{"platform":"%s","pushToken":"%s","deviceName":"Telefonul lui Marius"}
				""".formatted(platform, pushToken);
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

		assertThat(deviceRepository.findByPushToken(TOKEN)).isPresent();
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
		assertThat(deviceRepository.findByPushToken(TOKEN).orElseThrow().getUserId())
				.isEqualTo(second.id());
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