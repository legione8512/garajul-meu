package ro.garajulmeu.registrationcertificate;

import java.time.Instant;
import java.util.UUID;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
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
import ro.garajulmeu.vehicle.Vehicle;
import ro.garajulmeu.vehicle.VehicleRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class RegistrationCertificateFlowTest {

	/** The four required fields and nothing else - a complete certificate per section 8. */
	private static final String MINIMAL = """
			{"registrationNumber": "B 100 ABC", "make": "Dacia",
			 "commercialDescription": "Logan", "vin": "VF1AAAAAAAA000001"}
			""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private VehicleRepository vehicleRepository;

	@Autowired
	private RegistrationCertificateRepository certificateRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AccessTokenService accessTokenService;

	/** Unused. Present only so this class shares AuthFlowTest's context. */
	@MockitoBean
	private EmailProvider emailProvider;


	private record Owned(UUID userId, UUID vehicleId, String token) {
	}

	private Owned givenVehicle(String email, String vin) {
		User user = new User("Marius Robert", email, passwordEncoder.encode("a-long-enough-password"));
		user.setEmailVerifiedAt(Instant.now());
		UUID userId = userRepository.saveAndFlush(user).getId();

		UUID vehicleId = givenSecondVehicle(userId, "B 100 ABC", "Dacia", "Logan", vin);

		return new Owned(userId, vehicleId, accessTokenService.issueFor(userId).value());
	}

	/** Also used on its own, for the account that needs two vehicles to clash. */
	private UUID givenSecondVehicle(UUID userId, String plate, String make, String model, String vin) {
		UUID vehicleId = vehicleRepository.saveAndFlush(new Vehicle(userId)).getId();
		certificateRepository.saveAndFlush(new RegistrationCertificate(
				vehicleId, userId, plate, make, model, vin));
		return vehicleId;
	}

	private String certificateOf(UUID vehicleId) {
		return "/api/v1/vehicles/" + vehicleId + "/registration-certificate";
	}

	@Test
	void theWholeCertificateComesBack() throws Exception {
		Owned owned = givenVehicle("read@example.com", "VF1AAAAAAAA000001");

		mockMvc.perform(get(certificateOf(owned.vehicleId()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + owned.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.registrationNumber").value("B 100 ABC"))
				.andExpect(jsonPath("$.vin").value("VF1AAAAAAAA000001"))
				// Present in the shape even when empty, so the editor can render every box.
				.andExpect(jsonPath("$").value(org.hamcrest.Matchers.hasKey("ownerAddress")))
				.andExpect(jsonPath("$").value(org.hamcrest.Matchers.hasKey("engineCapacityCc")));
	}

	@Test
	void aCorrectionIsStoredAndReadBack() throws Exception {
		Owned owned = givenVehicle("correct@example.com", "VF1AAAAAAAA000001");

		mockMvc.perform(patch(certificateOf(owned.vehicleId()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + owned.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"registrationNumber": "B 100 ABC", "make": "Dacia",
								 "commercialDescription": "Logan", "vin": "VF1AAAAAAAA000001",
								 "colour": "albastru", "seats": 5, "engineCapacityCc": 999,
								 "maximumPowerKw": 66.00, "firstRegistrationDate": "2019-03-14"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.colour").value("albastru"));

		mockMvc.perform(get(certificateOf(owned.vehicleId()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + owned.token()))
				.andExpect(jsonPath("$.seats").value(5))
				.andExpect(jsonPath("$.engineCapacityCc").value(999))
				.andExpect(jsonPath("$.firstRegistrationDate").value("2019-03-14"));
	}

	/**
	 * The semantics decision, asserted rather than described: this endpoint
	 * replaces the optional block, so a body that omits a field clears it. If
	 * that ever becomes a real merge, this is the test that will say so.
	 */
	@Test
	void aBodyThatOmitsAnOptionalFieldClearsIt() throws Exception {
		Owned owned = givenVehicle("replace@example.com", "VF1AAAAAAAA000001");

		mockMvc.perform(patch(certificateOf(owned.vehicleId()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + owned.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"registrationNumber": "B 100 ABC", "make": "Dacia",
								 "commercialDescription": "Logan", "vin": "VF1AAAAAAAA000001",
								 "colour": "albastru"}
								"""))
				.andExpect(jsonPath("$.colour").value("albastru"));

		mockMvc.perform(patch(certificateOf(owned.vehicleId()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + owned.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(MINIMAL))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.colour").doesNotExist());
	}

	@Test
	void aBlankRequiredFieldIsRefusedByName() throws Exception {
		Owned owned = givenVehicle("blank@example.com", "VF1AAAAAAAA000001");

		mockMvc.perform(patch(certificateOf(owned.vehicleId()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + owned.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"registrationNumber": "B 100 ABC", "make": "  ",
								 "commercialDescription": "Logan", "vin": "VF1AAAAAAAA000001"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("make"));
	}

	/**
	 * Without @Digits this value reaches a DECIMAL(8,2) column, fails in
	 * PostgreSQL and answers 500 - blaming us for a number the caller typed.
	 */
	@Test
	void aNumberTooLargeForItsColumnIsTheCallersMistakeAndNotOurs() throws Exception {
		Owned owned = givenVehicle("precision@example.com", "VF1AAAAAAAA000001");

		mockMvc.perform(patch(certificateOf(owned.vehicleId()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + owned.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"registrationNumber": "B 100 ABC", "make": "Dacia",
								 "commercialDescription": "Logan", "vin": "VF1AAAAAAAA000001",
								 "maximumPowerKw": 123456789.99}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	/** Section 9 has to hold after the front door, not only at it. */
	@Test
	void correctingTheVinIntoOneAlreadyInTheGarageIsRefused() throws Exception {
		Owned owned = givenVehicle("vin-clash@example.com", "VF1AAAAAAAA000001");
		givenSecondVehicle(owned.userId(), "CJ 200 XYZ", "Volkswagen", "Golf", "VF1AAAAAAAA000002");;

		mockMvc.perform(patch(certificateOf(owned.vehicleId()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + owned.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"registrationNumber": "B 100 ABC", "make": "Dacia",
								 "commercialDescription": "Logan", "vin": "VF1AAAAAAAA000002"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("VEHICLE_DUPLICATE_VIN"));
	}

	/**
	 * The other half of that rule. Without the "has it changed" guard, every
	 * correction of a colour would be refused as a duplicate of itself.
	 */
	@Test
	void keepingTheSameVinIsNotADuplicateOfItself() throws Exception {
		Owned owned = givenVehicle("same-vin@example.com", "VF1AAAAAAAA000001");

		mockMvc.perform(patch(certificateOf(owned.vehicleId()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + owned.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"registrationNumber": "B 100 ABC", "make": "Dacia",
								 "commercialDescription": "Logan", "vin": " vf1aaaa aaaa000001 ",
								 "colour": "rosu"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.vin").value("VF1AAAAAAAA000001"))
				.andExpect(jsonPath("$.colour").value("rosu"));
	}

	/**
	 * Section 8: C.2 and C.3 are stored to fulfil the digital-certificate
	 * function and must never be logged. Asserted by listening to every logger
	 * for the duration of the request, because a ban nobody checks is a ban that
	 * lasts until the next person adds a helpful debug line.
	 */
	@Test
	void theOwnersAddressIsStoredAndNeverLogged() throws Exception {
		Owned owned = givenVehicle("sensitive@example.com", "VF1AAAAAAAA000001");

		ListAppender<ILoggingEvent> recorded = new ListAppender<>();
		Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		recorded.start();
		root.addAppender(recorded);

		try {
			mockMvc.perform(patch(certificateOf(owned.vehicleId()))
							.header(HttpHeaders.AUTHORIZATION, "Bearer " + owned.token())
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"registrationNumber": "B 100 ABC", "make": "Dacia",
									 "commercialDescription": "Logan", "vin": "VF1AAAAAAAA000001",
									 "ownerNameOrCompany": "Robert", "ownerFirstName": "Marius",
									 "ownerAddress": "Str. Confidentiala 7, Bucuresti",
									 "c2EqualsC1": true}
									"""))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.ownerAddress").value("Str. Confidentiala 7, Bucuresti"))
					.andExpect(jsonPath("$.c2EqualsC1").value(true));
		} finally {
			root.detachAppender(recorded);
		}

		assertThat(recorded.list)
				.extracting(ILoggingEvent::getFormattedMessage)
				.noneMatch(message -> message.contains("Confidentiala"))
				.noneMatch(message -> message.contains("ownerAddress"));
	}

	@Test
	void somebodyElsesCertificateIsNotFound() throws Exception {
		Owned owner = givenVehicle("cert-owner@example.com", "VF1AAAAAAAA000001");
		Owned stranger = givenVehicle("cert-stranger@example.com", "VF1AAAAAAAA000002");

		mockMvc.perform(get(certificateOf(owner.vehicleId()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + stranger.token()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("VEHICLE_NOT_FOUND"));

		mockMvc.perform(patch(certificateOf(owner.vehicleId()))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + stranger.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(MINIMAL))
				.andExpect(status().isNotFound());
	}

	@Test
	void bothEndpointsRefuseACallerWithNoToken() throws Exception {
		mockMvc.perform(get(certificateOf(UUID.randomUUID())))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(patch(certificateOf(UUID.randomUUID()))
						.contentType(MediaType.APPLICATION_JSON).content(MINIMAL))
				.andExpect(status().isUnauthorized());
	}
}