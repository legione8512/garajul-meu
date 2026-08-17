package ro.garajulmeu.vehicledocument;

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
import org.springframework.http.MediaType;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class VehicleDocumentFlowTest {

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

	/** The vehicle is flushed before its certificate, for the reason VehicleService gives. */
	private UUID givenVehicle(UUID accountId, String vin) {
		Vehicle vehicle = vehicleRepository.saveAndFlush(new Vehicle(accountId));
		certificateRepository.saveAndFlush(new RegistrationCertificate(
				vehicle.getId(), accountId, "B 100 ABC", "Dacia", "Logan", vin));
		return vehicle.getId();
	}

	/**
	 * The same call the service makes, never a bare LocalDate.now(): the identical
	 * shortcut in OcrQuota's tests failed at 01:19 having been green for weeks.
	 */
	private LocalDate today() {
		return DocumentCoverage.todayFor(clock, ZoneId.of("Europe/Bucharest"));
	}

	private String body(String type, LocalDate validFrom, LocalDate validUntil, String extra) {
		return """
				{"type":"%s","validFrom":%s,"validUntil":"%s"%s}
				""".formatted(type,
				validFrom == null ? "null" : "\"" + validFrom + "\"",
				validUntil, extra);
	}

	private String path(UUID vehicleId) {
		return "/api/v1/vehicles/" + vehicleId + "/documents";
	}

	@Test
	void aDocumentIsAddedAndComesBackInTheList() throws Exception {
		Account account = givenAccount("documents@example.com");
		UUID vehicleId = givenVehicle(account.id(), "VF1AAAAAAAA000001");

		mockMvc.perform(post(path(vehicleId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body("RCA", today(), today().plusDays(200), ",\"provider\":\"Allianz\"")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.type").value("RCA"))
				.andExpect(jsonPath("$.provider").value("Allianz"));

		mockMvc.perform(get(path(vehicleId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].provider").value("Allianz"));
	}

	/**
	 * Section 11's status is computed for the request rather than stored, so a
	 * document five days from expiry is URGENT without anything having been
	 * written to say so.
	 */
	@Test
	void theStatusAndTheDaysAreComputedRatherThanStored() throws Exception {
		Account account = givenAccount("status@example.com");
		UUID vehicleId = givenVehicle(account.id(), "VF1AAAAAAAA000002");

		mockMvc.perform(post(path(vehicleId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body("ITP", null, today().plusDays(5), "")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("URGENT"))
				.andExpect(jsonPath("$.daysRemaining").value(5));
	}

	/** Section 12: a period that ends before it starts is refused by name. */
	@Test
	void aPeriodThatEndsBeforeItStartsIsRefused() throws Exception {
		Account account = givenAccount("backwards@example.com");
		UUID vehicleId = givenVehicle(account.id(), "VF1AAAAAAAA000003");

		mockMvc.perform(post(path(vehicleId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body("CASCO", today().plusDays(10), today(), "")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("DOCUMENT_INVALID_DATE_RANGE"));

		assertThat(documentRepository.count()).isZero();
	}

	/**
	 * The reason the request carries the type as text. Taking the enum directly
	 * would let Jackson refuse the body first, and the caller would be told the
	 * whole request was malformed when one named field was wrong.
	 */
	@Test
	void anUnrecognisedTypeIsNamedRatherThanCalledMalformed() throws Exception {
		Account account = givenAccount("badtype@example.com");
		UUID vehicleId = givenVehicle(account.id(), "VF1AAAAAAAA000004");

		mockMvc.perform(post(path(vehicleId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body("VIGNETTE", null, today().plusDays(30), "")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("DOCUMENT_TYPE_INVALID"));
	}

	/** Lower case is the same type; a dropdown sends one shape, a script another. */
	@Test
	void aTypeInAnyCaseIsTheSameType() throws Exception {
		Account account = givenAccount("case@example.com");
		UUID vehicleId = givenVehicle(account.id(), "VF1AAAAAAAA000005");

		mockMvc.perform(post(path(vehicleId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body("rovinieta", null, today().plusDays(30), "")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.type").value("ROVINIETA"));
	}

	/**
	 * A correction replaces the record. The note is not mentioned in the second
	 * body, so it is cleared - which is the only way screen 13 can delete one.
	 */
	@Test
	void aCorrectionReplacesTheRecordRatherThanPatchingIt() throws Exception {
		Account account = givenAccount("correct@example.com");
		UUID vehicleId = givenVehicle(account.id(), "VF1AAAAAAAA000006");

		String created = mockMvc.perform(post(path(vehicleId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body("RCA", null, today().plusDays(100), ",\"notes\":\"pe hârtie\"")))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();

		UUID documentId = UUID.fromString(created.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1"));

		mockMvc.perform(patch(path(vehicleId) + "/" + documentId)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body("RCA", null, today().plusDays(365), "")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.notes").doesNotExist());

		assertThat(documentRepository.findById(documentId).orElseThrow().getNotes()).isNull();
	}

	@Test
	void deletingRemovesTheDocument() throws Exception {
		Account account = givenAccount("delete@example.com");
		UUID vehicleId = givenVehicle(account.id(), "VF1AAAAAAAA000007");

		VehicleDocument document = documentRepository.saveAndFlush(
				new VehicleDocument(vehicleId, DocumentType.RCA, today().plusDays(50)));

		mockMvc.perform(delete(path(vehicleId) + "/" + document.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token()))
				.andExpect(status().isNoContent());

		assertThat(documentRepository.findById(document.getId())).isEmpty();
	}

	/**
	 * Section 15: knowing a UUID is never enough. Both identifiers are matched
	 * against the account in SQL, so another account's vehicle answers exactly as
	 * one that does not exist.
	 */
	@Test
	void anotherAccountsVehicleIsIndistinguishableFromOneThatDoesNotExist() throws Exception {
		Account owner = givenAccount("owner@example.com");
		Account stranger = givenAccount("stranger@example.com");
		UUID vehicleId = givenVehicle(owner.id(), "VF1AAAAAAAA000008");

		VehicleDocument document = documentRepository.saveAndFlush(
				new VehicleDocument(vehicleId, DocumentType.ITP, today().plusDays(50)));

		mockMvc.perform(get(path(vehicleId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + stranger.token()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("VEHICLE_NOT_FOUND"));

		mockMvc.perform(get(path(vehicleId) + "/" + document.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + stranger.token()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("DOCUMENT_NOT_FOUND"));

		assertThat(documentRepository.findById(document.getId())).isPresent();
	}

	/**
	 * A document belonging to another of the caller's own vehicles is still not
	 * this vehicle's. The identifiers are matched as a pair, not one at a time.
	 */
	@Test
	void aDocumentOfAnotherVehicleIsNotFoundUnderThisOne() throws Exception {
		Account account = givenAccount("pairs@example.com");
		UUID first = givenVehicle(account.id(), "VF1AAAAAAAA000009");
		UUID second = givenVehicle(account.id(), "VF1AAAAAAAA000010");

		VehicleDocument document = documentRepository.saveAndFlush(
				new VehicleDocument(first, DocumentType.RCA, today().plusDays(50)));

		mockMvc.perform(get(path(second) + "/" + document.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("DOCUMENT_NOT_FOUND"));
	}

	/**
	 * The foreign key carries them away, as it carries a certificate away with its
	 * vehicle - there is no mapped association for Hibernate to cascade along.
	 */
	@Test
	void deletingTheVehicleTakesItsDocumentsWithIt() throws Exception {
		Account account = givenAccount("cascade@example.com");
		UUID vehicleId = givenVehicle(account.id(), "VF1AAAAAAAA000011");

		documentRepository.saveAndFlush(
				new VehicleDocument(vehicleId, DocumentType.RCA, today().plusDays(50)));

		mockMvc.perform(delete("/api/v1/vehicles/" + vehicleId)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token()))
				.andExpect(status().isNoContent());

		assertThat(documentRepository.count()).isZero();
	}

	@Test
	void everyEndpointRefusesACallerWithNoToken() throws Exception {
		UUID vehicleId = UUID.randomUUID();
		UUID documentId = UUID.randomUUID();

		mockMvc.perform(get(path(vehicleId))).andExpect(status().isUnauthorized());
		mockMvc.perform(get(path(vehicleId) + "/" + documentId)).andExpect(status().isUnauthorized());
		mockMvc.perform(post(path(vehicleId)).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(patch(path(vehicleId) + "/" + documentId)
						.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(delete(path(vehicleId) + "/" + documentId)).andExpect(status().isUnauthorized());
	}
}