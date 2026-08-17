package ro.garajulmeu.dashboard;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import ro.garajulmeu.TestcontainersConfiguration;
import ro.garajulmeu.email.EmailProvider;
import ro.garajulmeu.registrationcertificate.RegistrationCertificate;
import ro.garajulmeu.registrationcertificate.RegistrationCertificateRepository;
import ro.garajulmeu.security.AccessTokenService;
import ro.garajulmeu.user.User;
import ro.garajulmeu.user.UserRepository;
import ro.garajulmeu.vehicle.Vehicle;
import ro.garajulmeu.vehicle.VehicleRepository;
import ro.garajulmeu.vehicledocument.DocumentCoverage;
import ro.garajulmeu.vehicledocument.DocumentType;
import ro.garajulmeu.vehicledocument.VehicleDocument;
import ro.garajulmeu.vehicledocument.VehicleDocumentRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class DashboardFlowTest {

	private static final String PATH = "/api/v1/dashboard";

	/** The order DocumentType declares, which is the order the lines arrive in. */
	private static final String RCA = "$.vehicles[0].documents[0]";
	private static final String CASCO = "$.vehicles[0].documents[1]";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private VehicleRepository vehicleRepository;

	@Autowired
	private RegistrationCertificateRepository certificateRepository;

	@Autowired
	private VehicleDocumentRepository documentRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AccessTokenService accessTokenService;

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

	private UUID givenVehicle(UUID accountId, String plate, String vin) {
		Vehicle vehicle = vehicleRepository.saveAndFlush(new Vehicle(accountId));
		certificateRepository.saveAndFlush(new RegistrationCertificate(
				vehicle.getId(), accountId, plate, "Dacia", "Logan", vin));
		return vehicle.getId();
	}

	private VehicleDocument given(UUID vehicleId, DocumentType type, LocalDate from, LocalDate until) {
		VehicleDocument document = new VehicleDocument(vehicleId, type, until);
		document.setValidFrom(from);
		return documentRepository.saveAndFlush(document);
	}

	/** The same call the service makes, never a bare LocalDate.now(). */
	private LocalDate today() {
		return DocumentCoverage.todayFor(clock, ZoneId.of("Europe/Bucharest"));
	}

	@Test
	void everyVehicleCarriesAllFourTypesWhetherConfiguredOrNot() throws Exception {
		Account account = givenAccount("dash@example.com");
		UUID vehicleId = givenVehicle(account.id(), "B 100 ABC", "VF1AAAAAAAA000020");
		given(vehicleId, DocumentType.RCA, today().minusDays(10), today().plusDays(100));

		mockMvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.vehicles.length()").value(1))
				.andExpect(jsonPath("$.vehicles[0].registrationNumber").value("B 100 ABC"))
				.andExpect(jsonPath("$.vehicles[0].documents.length()").value(4))
				.andExpect(jsonPath(RCA + ".status").value("ACTIVE"))
				.andExpect(jsonPath(RCA + ".daysRemaining").value(100));
	}

	/** Section 11's one presentation-only state: no record has ever existed. */
	@Test
	void aTypeNothingWasEverEnteredForIsNotConfigured() throws Exception {
		Account account = givenAccount("unconfigured@example.com");
		UUID vehicleId = givenVehicle(account.id(), "B 101 ABC", "VF1AAAAAAAA000021");
		given(vehicleId, DocumentType.RCA, null, today().plusDays(100));

		mockMvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token()))
				.andExpect(jsonPath(CASCO + ".status").value("NOT_CONFIGURED"))
				.andExpect(jsonPath(CASCO + ".documentId").doesNotExist())
				.andExpect(jsonPath(CASCO + ".validUntil").doesNotExist())
				.andExpect(jsonPath(CASCO + ".daysRemaining").doesNotExist());
	}

	/** Greatest valid_from wins, so a renewal speaks for the vehicle without being marked. */
	@Test
	void theRenewalDecidesTheStatusWithoutAnythingBeingMarked() throws Exception {
		Account account = givenAccount("renewed@example.com");
		UUID vehicleId = givenVehicle(account.id(), "B 102 ABC", "VF1AAAAAAAA000022");

		given(vehicleId, DocumentType.RCA, today().minusDays(100), today().plusDays(3));
		VehicleDocument renewal =
				given(vehicleId, DocumentType.RCA, today().minusDays(1), today().plusDays(300));

		mockMvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token()))
				.andExpect(jsonPath(RCA + ".documentId").value(renewal.getId().toString()))
				.andExpect(jsonPath(RCA + ".status").value("ACTIVE"))
				.andExpect(jsonPath(RCA + ".daysRemaining").value(300));
	}

	/**
	 * Section 11 in its own words: "the UI must show the gap/expired state rather
	 * than treating the future record as active". The lapse is the answer; the
	 * future policy is a second fact beside it, not a replacement for it.
	 */
	@Test
	void aLapseWithCoverAlreadyBoughtReadsAsTheGapAndNamesWhenItResumes() throws Exception {
		Account account = givenAccount("gap@example.com");
		UUID vehicleId = givenVehicle(account.id(), "B 103 ABC", "VF1AAAAAAAA000023");

		given(vehicleId, DocumentType.RCA, today().minusDays(370), today().minusDays(5));
		given(vehicleId, DocumentType.RCA, today().plusDays(10), today().plusDays(375));

		mockMvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token()))
				.andExpect(jsonPath(RCA + ".status").value("EXPIRED"))
				.andExpect(jsonPath(RCA + ".validUntil").value(today().minusDays(5).toString()))
				.andExpect(jsonPath(RCA + ".daysRemaining").value(-5))
				.andExpect(jsonPath(RCA + ".upcomingFrom").value(today().plusDays(10).toString()));
	}

	/**
	 * A policy bought and not yet started. Section 11 gives no status meaning "not
	 * started", so the honest answer to the only question the dashboard asks - are
	 * you covered today - is no, and the absent {@code validUntil} beside a
	 * present {@code upcomingFrom} is what tells the screen this is not a lapse.
	 */
	@Test
	void aPolicyThatHasNotStartedIsNotReportedAsActive() throws Exception {
		Account account = givenAccount("future@example.com");
		UUID vehicleId = givenVehicle(account.id(), "B 104 ABC", "VF1AAAAAAAA000024");

		given(vehicleId, DocumentType.RCA, today().plusDays(10), today().plusDays(375));

		mockMvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token()))
				.andExpect(jsonPath(RCA + ".status").value("EXPIRED"))
				.andExpect(jsonPath(RCA + ".validUntil").doesNotExist())
				.andExpect(jsonPath(RCA + ".documentId").doesNotExist())
				.andExpect(jsonPath(RCA + ".upcomingFrom").value(today().plusDays(10).toString()));
	}

	@Test
	void theDashboardDescribesOnlyTheTokensGarage() throws Exception {
		Account mine = givenAccount("mine@example.com");
		Account theirs = givenAccount("theirs@example.com");

		UUID hers = givenVehicle(theirs.id(), "B 105 ABC", "VF1AAAAAAAA000025");
		given(hers, DocumentType.RCA, null, today().plusDays(100));

		mockMvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, "Bearer " + mine.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.vehicles.length()").value(0));
	}

	@Test
	void itRefusesACallerWithNoToken() throws Exception {
		mockMvc.perform(get(PATH)).andExpect(status().isUnauthorized());
	}
}