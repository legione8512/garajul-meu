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
	
	private String renewBody(LocalDate validFrom, LocalDate validUntil, String extra) {
		return """
				{"validFrom":%s,"validUntil":"%s"%s}
				""".formatted(validFrom == null ? "null" : "\"" + validFrom + "\"", validUntil, extra);
	}

	/** Section 12: a renewal is a new row, and the old one is left exactly as it was. */
	@Test
	void renewingCreatesANewRecordAndLeavesTheOldOneUntouched() throws Exception {
		Account account = givenAccount("renew@example.com");
		UUID vehicleId = givenVehicle(account.id(), "VF1AAAAAAAA000012");

		VehicleDocument original = new VehicleDocument(vehicleId, DocumentType.RCA, today().plusDays(10));
		original.setValidFrom(today().minusDays(355));
		documentRepository.saveAndFlush(original);

		mockMvc.perform(post(path(vehicleId) + "/" + original.getId() + "/renew")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(renewBody(today().plusDays(11), today().plusDays(376), "")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.type").value("RCA"))
				.andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.not(original.getId().toString())));

		assertThat(documentRepository.count()).isEqualTo(2);

		VehicleDocument stillThere = documentRepository.findById(original.getId()).orElseThrow();
		assertThat(stillThere.getValidUntil()).isEqualTo(today().plusDays(10));
		assertThat(stillThere.getValidFrom()).isEqualTo(today().minusDays(355));
	}

	/**
	 * The type carries over because that is what makes it a renewal. The policy
	 * number does not, because a new period has a new one - and two records
	 * claiming the same policy would be a false history.
	 */
	@Test
	void aRenewalKeepsTheTypeAndInheritsNothingElse() throws Exception {
		Account account = givenAccount("inherit@example.com");
		UUID vehicleId = givenVehicle(account.id(), "VF1AAAAAAAA000013");

		VehicleDocument original = new VehicleDocument(vehicleId, DocumentType.CASCO, today().plusDays(10));
		original.setProvider("Allianz");
		original.setReferenceNumber("POL-12345");
		original.setNotes("pe hârtie");
		documentRepository.saveAndFlush(original);

		mockMvc.perform(post(path(vehicleId) + "/" + original.getId() + "/renew")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(renewBody(null, today().plusDays(376), "")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.type").value("CASCO"))
				.andExpect(jsonPath("$.provider").doesNotExist())
				.andExpect(jsonPath("$.referenceNumber").doesNotExist())
				.andExpect(jsonPath("$.notes").doesNotExist());
	}

	/**
	 * Section 11 sets out how to choose between overlapping records rather than
	 * forbidding them, so a renewal that overlaps is data the model can already
	 * read - and refusing it would be inventing a rule the specification declined
	 * to state.
	 */
	@Test
	void anOverlappingRenewalIsAcceptedBecauseSectionElevenResolvesOverlaps() throws Exception {
		Account account = givenAccount("overlap@example.com");
		UUID vehicleId = givenVehicle(account.id(), "VF1AAAAAAAA000014");

		VehicleDocument original = new VehicleDocument(vehicleId, DocumentType.RCA, today().plusDays(100));
		original.setValidFrom(today().minusDays(100));
		documentRepository.saveAndFlush(original);

		mockMvc.perform(post(path(vehicleId) + "/" + original.getId() + "/renew")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(renewBody(today().minusDays(10), today().plusDays(300), "")))
				.andExpect(status().isCreated());

		assertThat(documentRepository.count()).isEqualTo(2);
	}

	/**
	 * The rule of section 12 applied through the other shape. It lives in one
	 * method precisely so that adding renewal could not let it drift.
	 */
	@Test
	void renewingRefusesAPeriodThatEndsBeforeItStarts() throws Exception {
		Account account = givenAccount("renewbackwards@example.com");
		UUID vehicleId = givenVehicle(account.id(), "VF1AAAAAAAA000015");

		VehicleDocument original = documentRepository.saveAndFlush(
				new VehicleDocument(vehicleId, DocumentType.ITP, today().plusDays(10)));

		mockMvc.perform(post(path(vehicleId) + "/" + original.getId() + "/renew")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(renewBody(today().plusDays(400), today().plusDays(300), "")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("DOCUMENT_INVALID_DATE_RANGE"));

		assertThat(documentRepository.count()).isEqualTo(1);
	}

	@Test
	void renewingAnotherAccountsDocumentIsNotFoundAndCreatesNothing() throws Exception {
		Account owner = givenAccount("renewowner@example.com");
		Account stranger = givenAccount("renewstranger@example.com");
		UUID vehicleId = givenVehicle(owner.id(), "VF1AAAAAAAA000016");

		VehicleDocument original = documentRepository.saveAndFlush(
				new VehicleDocument(vehicleId, DocumentType.RCA, today().plusDays(10)));

		mockMvc.perform(post(path(vehicleId) + "/" + original.getId() + "/renew")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + stranger.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(renewBody(null, today().plusDays(376), "")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("DOCUMENT_NOT_FOUND"));

		assertThat(documentRepository.count()).isEqualTo(1);
	}
	
	private String historyPath(UUID vehicleId) {
		return "/api/v1/vehicles/" + vehicleId + "/history";
	}

	/**
	 * The history is the records themselves - section 1 asks for renewal history
	 * "without overwriting previous records", and section 10 declares no event log
	 * to keep one in. This is the same assertion as 10.3's, read from the other
	 * end: what renewal declines to touch is exactly what history shows.
	 */
	@Test
	void theHistoryKeepsSupersededRecordsNewestEntryFirst() throws Exception {
		Account account = givenAccount("history@example.com");
		UUID vehicleId = givenVehicle(account.id(), "VF1AAAAAAAA000030");

		VehicleDocument original = documentRepository.saveAndFlush(
				new VehicleDocument(vehicleId, DocumentType.RCA, today().plusDays(10)));

		mockMvc.perform(post(path(vehicleId) + "/" + original.getId() + "/renew")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(renewBody(null, today().plusDays(376), "")))
				.andExpect(status().isCreated());

		mockMvc.perform(get(historyPath(vehicleId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.items.length()").value(2))
				.andExpect(jsonPath("$.items[1].id").value(original.getId().toString()));
	}

	@Test
	void theHistoryCanBeNarrowedToOneType() throws Exception {
		Account account = givenAccount("filter@example.com");
		UUID vehicleId = givenVehicle(account.id(), "VF1AAAAAAAA000031");

		documentRepository.saveAndFlush(new VehicleDocument(vehicleId, DocumentType.RCA, today().plusDays(10)));
		documentRepository.saveAndFlush(new VehicleDocument(vehicleId, DocumentType.ITP, today().plusDays(20)));

		mockMvc.perform(get(historyPath(vehicleId) + "?type=ITP")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.items[0].type").value("ITP"));
	}

	/** The same code the write path uses, so a typo in a filter is not silently everything. */
	@Test
	void anUnrecognisedFilterIsNamedRatherThanIgnored() throws Exception {
		Account account = givenAccount("badfilter@example.com");
		UUID vehicleId = givenVehicle(account.id(), "VF1AAAAAAAA000032");

		mockMvc.perform(get(historyPath(vehicleId) + "?type=VIGNETTE")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("DOCUMENT_TYPE_INVALID"));
	}

	/**
	 * Without a ceiling, one request asks for every row a vehicle has ever had.
	 * The answer is the most it may have rather than an error, because pagination
	 * is navigation and a caller moving through a list should not have to handle a
	 * failure for asking to move too far.
	 */
	@Test
	void anOversizedPageIsClampedRatherThanRefused() throws Exception {
		Account account = givenAccount("clamp@example.com");
		UUID vehicleId = givenVehicle(account.id(), "VF1AAAAAAAA000033");

		documentRepository.saveAndFlush(new VehicleDocument(vehicleId, DocumentType.RCA, today().plusDays(10)));

		mockMvc.perform(get(historyPath(vehicleId) + "?size=5000&page=-3")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.size").value(100))
				.andExpect(jsonPath("$.page").value(0));
	}

	@Test
	void theSecondPageCarriesTheRestAndTheTotalsAgree() throws Exception {
		Account account = givenAccount("paging@example.com");
		UUID vehicleId = givenVehicle(account.id(), "VF1AAAAAAAA000034");

		for (int i = 1; i <= 3; i++) {
			documentRepository.saveAndFlush(
					new VehicleDocument(vehicleId, DocumentType.RCA, today().plusDays(i * 10)));
		}

		mockMvc.perform(get(historyPath(vehicleId) + "?size=2&page=0")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token()))
				.andExpect(jsonPath("$.items.length()").value(2))
				.andExpect(jsonPath("$.totalElements").value(3))
				.andExpect(jsonPath("$.totalPages").value(2));

		mockMvc.perform(get(historyPath(vehicleId) + "?size=2&page=1")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token()))
				.andExpect(jsonPath("$.items.length()").value(1))
				.andExpect(jsonPath("$.page").value(1));
	}

	@Test
	void anotherAccountsHistoryIsNotFound() throws Exception {
		Account owner = givenAccount("histowner@example.com");
		Account stranger = givenAccount("histstranger@example.com");
		UUID vehicleId = givenVehicle(owner.id(), "VF1AAAAAAAA000035");

		documentRepository.saveAndFlush(new VehicleDocument(vehicleId, DocumentType.RCA, today().plusDays(10)));

		mockMvc.perform(get(historyPath(vehicleId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + stranger.token()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("VEHICLE_NOT_FOUND"));
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
		mockMvc.perform(post(path(vehicleId) + "/" + documentId + "/renew")
						.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(delete(path(vehicleId) + "/" + documentId)).andExpect(status().isUnauthorized());
		mockMvc.perform(get(historyPath(vehicleId))).andExpect(status().isUnauthorized());
	}
}