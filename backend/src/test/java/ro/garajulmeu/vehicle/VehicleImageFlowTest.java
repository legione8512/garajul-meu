package ro.garajulmeu.vehicle;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

import javax.imageio.ImageIO;

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
import ro.garajulmeu.email.EmailProvider;
import ro.garajulmeu.registrationcertificate.RegistrationCertificate;
import ro.garajulmeu.registrationcertificate.RegistrationCertificateRepository;
import ro.garajulmeu.security.AccessTokenService;
import ro.garajulmeu.storage.FileStorageProvider;
import ro.garajulmeu.user.User;
import ro.garajulmeu.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Annotations identical to AuthFlowTest, including the unused EmailProvider
 * mock, so the cached context is reused and no further container starts.
 *
 * <p>The storage provider is the real local one, writing under {@code target/}.
 * Not mocked: a {@code @MockitoBean} would change the context configuration and
 * start a fifth container, and a filesystem is the one dependency that is
 * cheaper to use than to fake.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class VehicleImageFlowTest {

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

	@Autowired
	private FileStorageProvider storage;

	/**
	 * Only ever used to clear. Rows removed by a database cascade are invisible to
	 * the persistence context, so a finder would answer from memory and report a
	 * vehicle that no longer exists.
	 */
	@PersistenceContext
	private EntityManager entityManager;

	/** Unused. Present only so this class shares AuthFlowTest's context. */
	@MockitoBean
	private EmailProvider emailProvider;

	private record Garage(UUID accountId, String token, UUID vehicleId) {
	}

	private Garage givenVehicle(String email, String vin) {
		User user = new User("Marius Robert", email,
				passwordEncoder.encode("a-long-enough-password"));
		user.setEmailVerifiedAt(Instant.now());
		UUID accountId = userRepository.saveAndFlush(user).getId();

		UUID vehicleId = vehicleRepository.saveAndFlush(new Vehicle(accountId)).getId();

		certificateRepository.saveAndFlush(new RegistrationCertificate(vehicleId, accountId,
				"B123ABC", "Dacia", "Logan", vin));

		return new Garage(accountId, accessTokenService.issueFor(accountId).value(), vehicleId);
	}

	private static byte[] photograph(String format) throws IOException {
		BufferedImage picture = new BufferedImage(800, 400, BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(picture, format, out);
		return out.toByteArray();
	}

	private String imagePath(Garage garage) {
		return "/api/v1/vehicles/" + garage.vehicleId() + "/image";
	}

	private String storedKeyOf(Garage garage) {
		return vehicleRepository.findById(garage.vehicleId()).orElseThrow().getImageObjectKey();
	}

	/** Uploads a photograph and answers the key it was stored under. */
	private String givenAPhotograph(Garage garage) throws Exception {
		mockMvc.perform(put(imagePath(garage))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + garage.token())
						.content(photograph("jpeg")))
				.andExpect(status().isNoContent());

		return storedKeyOf(garage);
	}

	@Test
	void anUploadedPhotographComesBackWithTheTypeReadFromItsBytes() throws Exception {
		Garage garage = givenVehicle("upload@example.com", "VIN00000000UPLOAD1");
		byte[] bytes = photograph("jpeg");

		mockMvc.perform(put(imagePath(garage))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + garage.token())
						.contentType(MediaType.APPLICATION_OCTET_STREAM)
						.content(bytes))
				.andExpect(status().isNoContent());

		mockMvc.perform(get(imagePath(garage))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + garage.token()))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.IMAGE_JPEG))
				.andExpect(content().bytes(bytes))
				.andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"));
	}

	/**
	 * The declared type is ignored and the measured one is stored. A PNG announced
	 * as a JPEG comes back labelled PNG, which is the whole point of reading the
	 * format from the bytes.
	 */
	@Test
	void theStoredTypeIsTheOneMeasuredRatherThanTheOneDeclared() throws Exception {
		Garage garage = givenVehicle("declared@example.com", "VIN000000DECLARED1");

		mockMvc.perform(put(imagePath(garage))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + garage.token())
						.contentType(MediaType.IMAGE_JPEG)
						.content(photograph("png")))
				.andExpect(status().isNoContent());

		mockMvc.perform(get(imagePath(garage))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + garage.token()))
				.andExpect(content().contentType(MediaType.IMAGE_PNG));
	}

	/** The vehicle response says there is one, and says so without naming the object. */
	@Test
	void theVehicleReportsWhetherItHasAPhotographAndNeverItsKey() throws Exception {
		Garage garage = givenVehicle("flag@example.com", "VIN000000000FLAG01");
		String vehiclePath = "/api/v1/vehicles/" + garage.vehicleId();

		mockMvc.perform(get(vehiclePath)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + garage.token()))
				.andExpect(jsonPath("$.hasImage").value(false));

		mockMvc.perform(put(imagePath(garage))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + garage.token())
						.content(photograph("jpeg")))
				.andExpect(status().isNoContent());

		String body = mockMvc.perform(get(vehiclePath)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + garage.token()))
				.andExpect(jsonPath("$.hasImage").value(true))
				.andReturn().getResponse().getContentAsString();

		assertThat(body).doesNotContain(storedKeyOf(garage));
	}

	/**
	 * A replacement writes a new key and removes the old object. Asserted on the
	 * store rather than on the row, because the row would look right either way -
	 * and an orphan per replacement is exactly the sort of leak nobody notices
	 * until a bill arrives.
	 */
	@Test
	void replacingWritesANewObjectAndRemovesTheOldOne() throws Exception {
		Garage garage = givenVehicle("replace@example.com", "VIN0000000REPLACE1");

		mockMvc.perform(put(imagePath(garage))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + garage.token())
						.content(photograph("jpeg")))
				.andExpect(status().isNoContent());

		String first = storedKeyOf(garage);

		mockMvc.perform(put(imagePath(garage))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + garage.token())
						.content(photograph("png")))
				.andExpect(status().isNoContent());

		String second = storedKeyOf(garage);

		assertThat(second).isNotEqualTo(first);
		assertThat(storage.get(first)).isEmpty();
		assertThat(storage.get(second)).isPresent();
	}

	@Test
	void deletingRemovesTheObjectAndDeletingAgainIsStillSuccessful() throws Exception {
		Garage garage = givenVehicle("remove@example.com", "VIN00000000REMOVE1");

		mockMvc.perform(put(imagePath(garage))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + garage.token())
						.content(photograph("jpeg")))
				.andExpect(status().isNoContent());

		String objectKey = storedKeyOf(garage);

		mockMvc.perform(delete(imagePath(garage))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + garage.token()))
				.andExpect(status().isNoContent());

		assertThat(storedKeyOf(garage)).isNull();
		assertThat(storage.get(objectKey)).isEmpty();

		mockMvc.perform(delete(imagePath(garage))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + garage.token()))
				.andExpect(status().isNoContent());
	}

	@Test
	void aVehicleWithNoPhotographHasNothingToServe() throws Exception {
		Garage garage = givenVehicle("none@example.com", "VIN0000000000NONE1");

		mockMvc.perform(get(imagePath(garage))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + garage.token()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
	}

	/** Section 15, on all three verbs: knowing the identifier is never enough. */
	@Test
	void anotherAccountCanNeitherReadNorReplaceNorDelete() throws Exception {
		Garage mine = givenVehicle("mine@example.com", "VIN000000000MINE01");
		Garage theirs = givenVehicle("theirs@example.com", "VIN00000000THEIRS1");

		mockMvc.perform(put(imagePath(mine))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + mine.token())
						.content(photograph("jpeg")))
				.andExpect(status().isNoContent());

		String stranger = "Bearer " + theirs.token();

		mockMvc.perform(get(imagePath(mine)).header(HttpHeaders.AUTHORIZATION, stranger))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("VEHICLE_NOT_FOUND"));

		mockMvc.perform(put(imagePath(mine)).header(HttpHeaders.AUTHORIZATION, stranger)
						.content(photograph("png")))
				.andExpect(status().isNotFound());

		mockMvc.perform(delete(imagePath(mine)).header(HttpHeaders.AUTHORIZATION, stranger))
				.andExpect(status().isNotFound());

		// And the photograph the owner uploaded is untouched by any of it.
		assertThat(storage.get(storedKeyOf(mine))).isPresent();
	}

	@Test
	void somethingThatIsNotAPhotographIsRefusedByType() throws Exception {
		Garage garage = givenVehicle("rubbish@example.com", "VIN0000000RUBBISH1");

		mockMvc.perform(put(imagePath(garage))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + garage.token())
						.content("MZ\u0090\u0000\u0003 not a car".getBytes()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("IMAGE_INVALID_TYPE"));

		assertThat(storedKeyOf(garage)).isNull();
	}

	/**
	 * The bound the raw body cost us, and the reason the controller reads rather
	 * than trusts. Seventy kilobytes against a limit of sixty-four, and the
	 * refusal arrives as a code the client can act on rather than as a closed
	 * connection.
	 *
	 * <p>{@code isContentTooLarge} rather than {@code isPayloadTooLarge}: RFC 9110
	 * renamed 413 and Spring deprecated the old matcher in 7.0. The production
	 * side was already on the new name - ErrorCode declares
	 * {@code IMAGE_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE)} - and only this test
	 * had been left behind.
	 */
	@Test
	void aBodyOverTheLimitIsRefusedBeforeItIsAllRead() throws Exception {
		Garage garage = givenVehicle("toobig@example.com", "VIN00000000TOOBIG1");

		mockMvc.perform(put(imagePath(garage))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + garage.token())
						.content(new byte[70 * 1024]))
				.andExpect(status().isContentTooLarge())
				.andExpect(jsonPath("$.code").value("IMAGE_TOO_LARGE"));

		assertThat(storedKeyOf(garage)).isNull();
	}

	@Test
	void aRequestWithoutATokenIsRefused() throws Exception {
		Garage garage = givenVehicle("anonymous@example.com", "VIN0000ANONYMOUS02");

		mockMvc.perform(get(imagePath(garage))).andExpect(status().isUnauthorized());
		mockMvc.perform(put(imagePath(garage)).content(photograph("jpeg")))
				.andExpect(status().isUnauthorized());
	}

	/**
	 * 12.3b, and the gap 12.3 left open. The foreign key takes the certificate and
	 * every other row that hangs off the vehicle, and it cannot reach the bucket -
	 * so without the explicit removal the photograph would outlive the vehicle
	 * forever, unreachable because the only record of its key went with the row.
	 *
	 * <p>Asserted on the store, because the database looks identical either way.
	 */
	@Test
	void deletingTheVehicleTakesItsPhotographWithIt() throws Exception {
		Garage garage = givenVehicle("scrapped@example.com", "VIN000000SCRAPPED1");

		String objectKey = givenAPhotograph(garage);
		assertThat(storage.get(objectKey)).isPresent();

		mockMvc.perform(delete("/api/v1/vehicles/" + garage.vehicleId())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + garage.token()))
				.andExpect(status().isNoContent());

		assertThat(vehicleRepository.findById(garage.vehicleId())).isEmpty();
		assertThat(storage.get(objectKey)).isEmpty();
	}

	/**
	 * The same rule one level up, and the one section 24 actually names. Deleting
	 * an account cascades to its vehicles, which is precisely what destroys the
	 * keys - so the collection has to happen before the delete, and this test is
	 * what fails if somebody later moves that line down for tidiness.
	 *
	 * <p>Two vehicles, and only one of them photographed: the account's other car
	 * proves the query returns keys rather than rows, and that a garage with a
	 * picture-less vehicle in it does not produce a null to trip over.
	 */
	@Test
	void deletingTheAccountTakesEveryPhotographInItsGarageWithIt() throws Exception {
		Garage garage = givenVehicle("closing@example.com", "VIN000000CLOSING01");

		UUID second = vehicleRepository.saveAndFlush(new Vehicle(garage.accountId())).getId();
		certificateRepository.saveAndFlush(new RegistrationCertificate(second, garage.accountId(),
				"B999XYZ", "Dacia", "Sandero", "VIN00000CLOSING02"));

		String objectKey = givenAPhotograph(garage);
		assertThat(storage.get(objectKey)).isPresent();

		mockMvc.perform(delete("/api/v1/users/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + garage.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"currentPassword\":\"a-long-enough-password\"}"))
				.andExpect(status().isNoContent());

		// The vehicles went by database cascade, which JPA never saw, and this test
		// loaded the Vehicle earlier through storedKeyOf. Without this line findById
		// answers out of the first-level cache and reports a row that is gone.
		entityManager.clear();

		assertThat(userRepository.findById(garage.accountId())).isEmpty();
		assertThat(vehicleRepository.findById(garage.vehicleId())).isEmpty();
		assertThat(storage.get(objectKey)).isEmpty();
	}
}