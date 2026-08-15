package ro.garajulmeu.vehicle;

import java.time.Instant;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Annotations identical to AuthFlowTest, including the unused EmailProvider
 * mock, so the cached context is reused and no further container starts.
 *
 * <p>Tokens are issued directly rather than obtained by logging in. The token is
 * real and the security filter verifies it exactly as it would any other, so
 * nothing is bypassed - it simply avoids paying for an Argon2 comparison in
 * every test of a feature that has nothing to do with passwords.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class VehicleReadTest {

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

	private User account(String email) {
		User user = new User("Marius Robert", email, passwordEncoder.encode("a-long-enough-password"));
		user.setEmailVerifiedAt(Instant.now());
		return userRepository.saveAndFlush(user);
	}

	private Vehicle vehicleFor(User owner, String registrationNumber, String vin) {
		Vehicle vehicle = vehicleRepository.saveAndFlush(new Vehicle(owner.getId()));
		certificateRepository.saveAndFlush(new RegistrationCertificate(
				vehicle.getId(), owner.getId(), registrationNumber, "Dacia", "Logan", vin));
		return vehicle;
	}

	private String tokenFor(User user) {
		return accessTokenService.issueFor(user.getId()).value();
	}

	@Test
	void theGarageListsOnlyTheCallersVehicles() throws Exception {
		User owner = account("owner@example.com");
		User stranger = account("stranger@example.com");
		vehicleFor(owner, "B 100 ABC", "VF1AAAAAAAA000001");
		vehicleFor(stranger, "CJ 200 XYZ", "VF1AAAAAAAA000002");

		mockMvc.perform(get("/api/v1/vehicles")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].registrationNumber").value("B 100 ABC"));
	}

	@Test
	void anAccountWithNoVehiclesGetsAnEmptyGarage() throws Exception {
		User owner = account("empty@example.com");

		mockMvc.perform(get("/api/v1/vehicles")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	/** Section 9: the identity comes from the certificate, never from the vehicle row. */
	@Test
	void detailsCarryTheIdentityHeldOnTheCertificate() throws Exception {
		User owner = account("details@example.com");
		Vehicle vehicle = vehicleFor(owner, "B 300 DEF", "VF1AAAAAAAA000003");

		mockMvc.perform(get("/api/v1/vehicles/" + vehicle.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.registrationNumber").value("B 300 DEF"))
				.andExpect(jsonPath("$.make").value("Dacia"))
				.andExpect(jsonPath("$.commercialDescription").value("Logan"))
				.andExpect(jsonPath("$.vin").value("VF1AAAAAAAA000003"));
	}

	/**
	 * Section 15, and the reason the answer is 404 rather than 403: a refusal
	 * that admits the vehicle exists is a way of discovering other people's
	 * vehicles one identifier at a time.
	 */
	@Test
	void somebodyElsesVehicleIsIndistinguishableFromOneThatDoesNotExist() throws Exception {
		User owner = account("real-owner@example.com");
		User stranger = account("curious@example.com");
		Vehicle vehicle = vehicleFor(owner, "B 400 GHI", "VF1AAAAAAAA000004");

		mockMvc.perform(get("/api/v1/vehicles/" + vehicle.getId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(stranger)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("VEHICLE_NOT_FOUND"));

		mockMvc.perform(get("/api/v1/vehicles/" + UUID.randomUUID())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(stranger)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("VEHICLE_NOT_FOUND"));
	}

	/** Otherwise a mistyped URL answers 500 and, from Phase 15, raises an alert. */
	@Test
	void anIdentifierThatIsNotAUuidIsTheCallersMistakeAndNotOurs() throws Exception {
		User owner = account("typo@example.com");

		mockMvc.perform(get("/api/v1/vehicles/not-a-uuid")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(owner)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void bothEndpointsRefuseACallerWithNoToken() throws Exception {
		mockMvc.perform(get("/api/v1/vehicles"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

		mockMvc.perform(get("/api/v1/vehicles/" + UUID.randomUUID()))
				.andExpect(status().isUnauthorized());
	}
}