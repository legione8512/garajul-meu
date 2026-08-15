package ro.garajulmeu.vehicle;

import java.time.Instant;

import com.jayway.jsonpath.JsonPath;

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
import ro.garajulmeu.registrationcertificate.RegistrationCertificateRepository;
import ro.garajulmeu.security.AccessTokenService;
import ro.garajulmeu.user.User;
import ro.garajulmeu.user.UserRepository;

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
class VehicleWriteTest {

	private static final String LOGAN = """
			{"registrationNumber": "B 100 ABC", "make": "Dacia",
			 "commercialDescription": "Logan", "vin": "VF1AAAAAAAA000001"}
			""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RegistrationCertificateRepository certificateRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AccessTokenService accessTokenService;

	/** Unused. Present only so this class shares AuthFlowTest's context. */
	@MockitoBean
	private EmailProvider emailProvider;

	private String tokenFor(String email) {
		User user = new User("Marius Robert", email, passwordEncoder.encode("a-long-enough-password"));
		user.setEmailVerifiedAt(Instant.now());
		return accessTokenService.issueFor(userRepository.saveAndFlush(user).getId()).value();
	}

	private String createVehicle(String token, String body) throws Exception {
		String response = mockMvc.perform(post("/api/v1/vehicles")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(response, "$.id");
	}

	@Test
	void creatingAVehicleStoresItWithItsCertificate() throws Exception {
		String token = tokenFor("creator@example.com");

		mockMvc.perform(post("/api/v1/vehicles")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(LOGAN))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").exists())
				.andExpect(jsonPath("$.registrationNumber").value("B 100 ABC"))
				.andExpect(jsonPath("$.make").value("Dacia"))
				.andExpect(jsonPath("$.vin").value("VF1AAAAAAAA000001"))
				.andExpect(jsonPath("$.displayName").doesNotExist());

		mockMvc.perform(get("/api/v1/vehicles")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(jsonPath("$.length()").value(1));
	}

	/** Section 9, first half. */
	@Test
	void theSameVinTwiceInOneAccountIsRefused() throws Exception {
		String token = tokenFor("twice@example.com");
		createVehicle(token, LOGAN);

		mockMvc.perform(post("/api/v1/vehicles")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"registrationNumber": "CJ 999 ZZZ", "make": "Dacia",
								 "commercialDescription": "Logan", "vin": "VF1AAAAAAAA000001"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("VEHICLE_DUPLICATE_VIN"));
	}

	/**
	 * Section 9, second half, and the reason the unique index is on
	 * (user_id, vin) rather than on vin alone. A car changes hands; V1 has no
	 * transfer, so the buyer adds it while the seller's row still exists.
	 */
	@Test
	void theSameVinInAnotherAccountIsAllowed() throws Exception {
		createVehicle(tokenFor("seller@example.com"), LOGAN);

		mockMvc.perform(post("/api/v1/vehicles")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor("buyer@example.com"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(LOGAN))
				.andExpect(status().isCreated());
	}

	/**
	 * Without normalisation the unique index is trivially defeated: it compares
	 * bytes, so one lower-case letter is enough to put the same car in the same
	 * garage twice. The stored value is asserted before the duplicate is
	 * attempted, deliberately - a constraint violation aborts the transaction,
	 * and anything read afterwards would fail for a reason unrelated to the test.
	 */
	@Test
	void aVinIsNormalisedSoCaseAndSpacingCannotSlipADuplicatePast() throws Exception {
		String token = tokenFor("normalise@example.com");
		String id = createVehicle(token, """
				{"registrationNumber": "  b 100  abc ", "make": "  Dacia  ",
				 "commercialDescription": "Logan", "vin": " vf1aaaa aaaa000001 "}
				""");

		mockMvc.perform(get("/api/v1/vehicles/" + id)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(jsonPath("$.vin").value("VF1AAAAAAAA000001"))
				.andExpect(jsonPath("$.registrationNumber").value("B 100 ABC"))
				.andExpect(jsonPath("$.make").value("Dacia"));

		mockMvc.perform(post("/api/v1/vehicles")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(LOGAN))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("VEHICLE_DUPLICATE_VIN"));
	}

	@Test
	void aMissingMandatoryFieldIsRefusedByName() throws Exception {
		mockMvc.perform(post("/api/v1/vehicles")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor("incomplete@example.com"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"registrationNumber": "B 100 ABC", "make": "Dacia",
								 "commercialDescription": "Logan"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("vin"))
				.andExpect(jsonPath("$.fieldErrors[0].constraint").value("NotBlank"));
	}

	/**
	 * A nickname can be removed as well as given. An empty string is the way to
	 * say so, because an absent field means "leave alone" everywhere else in this
	 * API and one convention is worth more than one extra affordance.
	 */
	@Test
	void theNicknameCanBeSetAndCleared() throws Exception {
		String token = tokenFor("nickname@example.com");
		String id = createVehicle(token, LOGAN);

		mockMvc.perform(patch("/api/v1/vehicles/" + id)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"displayName\": \"  Mașina de teren  \"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.displayName").value("Mașina de teren"));

		mockMvc.perform(patch("/api/v1/vehicles/" + id)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"displayName\": \"\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.displayName").doesNotExist());

		mockMvc.perform(patch("/api/v1/vehicles/" + id)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.registrationNumber").value("B 100 ABC"));
	}

	/**
	 * Counted in the database rather than assumed. Nothing in Java removes the
	 * certificate - only the foreign key does - so an assertion that the vehicle
	 * is gone would say nothing about the row that carries its VIN, and a
	 * certificate outliving its vehicle would keep the VIN occupied and refuse
	 * the owner's next attempt to add the same car.
	 */
	@Test
	void deletingAVehicleTakesItsCertificateWithIt() throws Exception {
		String token = tokenFor("deleter@example.com");
		String id = createVehicle(token, LOGAN);
		assertThat(certificateRepository.count()).isEqualTo(1);

		mockMvc.perform(delete("/api/v1/vehicles/" + id)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isNoContent());

		assertThat(certificateRepository.count()).isZero();
		mockMvc.perform(get("/api/v1/vehicles/" + id)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isNotFound());
	}

	@Test
	void deletingSomebodyElsesVehicleChangesNothing() throws Exception {
		String owner = tokenFor("keeper@example.com");
		String id = createVehicle(owner, LOGAN);

		mockMvc.perform(delete("/api/v1/vehicles/" + id)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor("thief@example.com")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("VEHICLE_NOT_FOUND"));

		mockMvc.perform(get("/api/v1/vehicles/" + id)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + owner))
				.andExpect(status().isOk());
	}

	@Test
	void renamingSomebodyElsesVehicleChangesNothing() throws Exception {
		String owner = tokenFor("named@example.com");
		String id = createVehicle(owner, LOGAN);

		mockMvc.perform(patch("/api/v1/vehicles/" + id)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor("meddler@example.com"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"displayName\": \"Not yours\"}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("VEHICLE_NOT_FOUND"));

		mockMvc.perform(get("/api/v1/vehicles/" + id)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + owner))
				.andExpect(jsonPath("$.displayName").doesNotExist());
	}

	@Test
	void allThreeWritesRefuseACallerWithNoToken() throws Exception {
		mockMvc.perform(post("/api/v1/vehicles")
						.contentType(MediaType.APPLICATION_JSON).content(LOGAN))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(patch("/api/v1/vehicles/" + java.util.UUID.randomUUID())
						.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(delete("/api/v1/vehicles/" + java.util.UUID.randomUUID()))
				.andExpect(status().isUnauthorized());
	}
}