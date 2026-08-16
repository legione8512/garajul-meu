package ro.garajulmeu.ocr;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import ro.garajulmeu.TestcontainersConfiguration;
import ro.garajulmeu.email.EmailProvider;
import ro.garajulmeu.security.AccessTokenService;
import ro.garajulmeu.user.User;
import ro.garajulmeu.user.UserRepository;
import ro.garajulmeu.vehicle.VehicleRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Annotations identical to AuthFlowTest, including the unused EmailProvider
 * mock, so the cached context is reused and no further container starts. The
 * stub OCR provider is the one configured for tests, so nothing here reaches
 * Google or needs an account.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class OcrFlowTest {

	private static final String PATH = "/api/v1/ocr/registration-certificate";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private VehicleRepository vehicleRepository;

	@Autowired
	private OcrUsageRepository usageRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AccessTokenService accessTokenService;

	@Autowired
	private OcrProperties properties;
	
	@Autowired
	private Clock clock;

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

	private static MockMultipartFile photograph() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(new BufferedImage(800, 400, BufferedImage.TYPE_INT_RGB), "jpeg", out);
		return new MockMultipartFile("image", "certificate.jpg", "image/jpeg", out.toByteArray());
	}

	private int spentToday(UUID accountId) {
		return usageRepository.findByUserIdAndUsageDate(accountId, OcrQuota.allowanceDay(clock))
				.map(OcrUsage::getRequestCount)
				.orElse(0);
	}

	@Test
	void aPhotographComesBackAsProposals() throws Exception {
		Account account = givenAccount("scan@example.com");

		mockMvc.perform(multipart(PATH).file(photograph())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fields.length()").value(CertificateCode.values().length))
				.andExpect(jsonPath("$.fields[?(@.field == 'registrationNumber')].value")
						.value("B 100 ABC"))
				.andExpect(jsonPath("$.fields[?(@.field == 'registrationNumber')].status")
						.value("DETECTED"));
	}

	/**
	 * Section 13's own example of why confidence alone is not enough: the stub
	 * reads a VIN ending in I, which no VIN contains, and reads it poorly as well.
	 */
	@Test
	void aReadingThatCannotBeTrueComesBackForReview() throws Exception {
		Account account = givenAccount("review@example.com");

		mockMvc.perform(multipart(PATH).file(photograph())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token()))
				.andExpect(jsonPath("$.fields[?(@.field == 'vin')].status").value("NEEDS_REVIEW"))
				.andExpect(jsonPath("$.fields[?(@.field == 'vin')].value").value((Object) null));
	}

	/** Section 16: "no vehicle save". The scan creates nothing at all. */
	@Test
	void scanningSavesNothing() throws Exception {
		Account account = givenAccount("saves-nothing@example.com");

		mockMvc.perform(multipart(PATH).file(photograph())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token()))
				.andExpect(status().isOk());

		assertThat(vehicleRepository.count()).isZero();
	}

	@Test
	void aSuccessfulScanSpendsOneRequest() throws Exception {
		Account account = givenAccount("counted@example.com");

		mockMvc.perform(multipart(PATH).file(photograph())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token()))
				.andExpect(status().isOk());

		assertThat(spentToday(account.id())).isEqualTo(1);
	}

	/**
	 * The ordering decision, asserted rather than described. A broken upload asked
	 * nothing of the provider, so it costs nothing - and if the two steps are ever
	 * swapped, this is what says so.
	 */
	@Test
	void aBrokenUploadCostsNoAllowance() throws Exception {
		Account account = givenAccount("broken@example.com");
		MockMultipartFile notAnImage =
				new MockMultipartFile("image", "certificate.jpg", "image/jpeg", "MZ not a photograph".getBytes());

		mockMvc.perform(multipart(PATH).file(notAnImage)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("OCR_FILE_INVALID"));

		assertThat(spentToday(account.id())).isZero();
	}

	@Test
	void theDailyAllowanceIsEnforcedOnTheEndpoint() throws Exception {
		Account account = givenAccount("exhausted@example.com");

		OcrUsage spent = new OcrUsage(account.id(), OcrQuota.allowanceDay(clock));
		for (int i = 0; i < properties.dailyLimit(); i++) {
			spent.increment();
		}
		usageRepository.saveAndFlush(spent);

		mockMvc.perform(multipart(PATH).file(photograph())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token()))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.code").value("OCR_RATE_LIMITED"));
	}

	/** A multipart request with no file is the caller's mistake, not a server failure. */
	@Test
	void aRequestWithNoFileIsRefusedWithoutBlamingUs() throws Exception {
		Account account = givenAccount("nofile@example.com");

		mockMvc.perform(multipart(PATH)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
	}

	@Test
	void theEndpointRefusesACallerWithNoToken() throws Exception {
		mockMvc.perform(multipart(PATH).file(photograph()))
				.andExpect(status().isUnauthorized());
	}
}