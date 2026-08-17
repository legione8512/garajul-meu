package ro.garajulmeu.reminder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
class ReminderViewFlowTest {

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
	private ReminderRepository reminderRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AccessTokenService accessTokenService;

	/** Unused. Present only so this class shares AuthFlowTest's context. */
	@MockitoBean
	private EmailProvider emailProvider;

	private record Garage(UUID accountId, String token, UUID vehicleId, UUID documentId) {
	}

	private Garage givenDocument(String email, String vin) {
		User user = new User("Marius Robert", email,
				passwordEncoder.encode("a-long-enough-password"));
		user.setEmailVerifiedAt(Instant.now());
		UUID accountId = userRepository.saveAndFlush(user).getId();

		Vehicle vehicle = new Vehicle(accountId);
		UUID vehicleId = vehicleRepository.saveAndFlush(vehicle).getId();

		certificateRepository.saveAndFlush(new RegistrationCertificate(vehicleId, accountId,
				"B123ABC", "Dacia", "Logan", vin));

		VehicleDocument document = new VehicleDocument(vehicleId, DocumentType.RCA,
				LocalDate.now().plusDays(60));
		UUID documentId = documentRepository.saveAndFlush(document).getId();

		return new Garage(accountId, accessTokenService.issueFor(accountId).value(),
				vehicleId, documentId);
	}

	private void givenReminder(UUID documentId, int offsetDays, Instant scheduledAt,
			ReminderStatus status) {
		Reminder reminder = new Reminder(documentId, offsetDays, scheduledAt);
		reminder.setStatus(status);

		if (status == ReminderStatus.SENT) {
			reminder.setSentAt(scheduledAt);
		}
		reminderRepository.saveAndFlush(reminder);
	}

	private String path(Garage garage) {
		return "/api/v1/vehicles/" + garage.vehicleId() + "/documents/" + garage.documentId()
				+ "/reminders";
	}

	/** Firing order, which is the order they matter in and not the order they were written. */
	@Test
	void theScheduleComesBackInTheOrderItWillFire() throws Exception {
		Garage garage = givenDocument("schedule@example.com", "VIN000000SCHEDULE1");
		Instant now = Instant.now();

		givenReminder(garage.documentId(), 1, now.plus(29, ChronoUnit.DAYS), ReminderStatus.PENDING);
		givenReminder(garage.documentId(), 30, now.plus(1, ChronoUnit.DAYS), ReminderStatus.PENDING);
		givenReminder(garage.documentId(), 7, now.plus(23, ChronoUnit.DAYS), ReminderStatus.PENDING);

		mockMvc.perform(get(path(garage))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + garage.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3))
				.andExpect(jsonPath("$[0].offsetDays").value(30))
				.andExpect(jsonPath("$[1].offsetDays").value(7))
				.andExpect(jsonPath("$[2].offsetDays").value(1))
				.andExpect(jsonPath("$[0].status").value("PENDING"))
				.andExpect(jsonPath("$[0].sentAt").doesNotExist());
	}

	/**
	 * A document corrected three times holds eighteen cancelled rows. They stay in
	 * the table, per section 12, and they stay out of this.
	 */
	@Test
	void cancelledRemindersAreKeptInTheTableAndLeftOutOfTheView() throws Exception {
		Garage garage = givenDocument("cancelled@example.com", "VIN00000CANCELLED1");
		Instant now = Instant.now();

		givenReminder(garage.documentId(), 30, now.plusSeconds(60), ReminderStatus.CANCELLED);
		givenReminder(garage.documentId(), 14, now.plusSeconds(120), ReminderStatus.CANCELLED);
		givenReminder(garage.documentId(), 7, now.plusSeconds(180), ReminderStatus.PENDING);

		mockMvc.perform(get(path(garage))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + garage.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].offsetDays").value(7));

		// The rows themselves are untouched: the view is a filter, not a deletion.
		org.assertj.core.api.Assertions
				.assertThat(reminderRepository
						.findByVehicleDocumentIdOrderByScheduledAt(garage.documentId()))
				.hasSize(3);
	}

	/** A sent reminder carries when, which is the whole of "was I told". */
	@Test
	void aSentReminderCarriesTheInstantItCompleted() throws Exception {
		Garage garage = givenDocument("sent@example.com", "VIN000000000SENT01");

		givenReminder(garage.documentId(), 30, Instant.now().minusSeconds(3600),
				ReminderStatus.SENT);

		mockMvc.perform(get(path(garage))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + garage.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].status").value("SENT"))
				.andExpect(jsonPath("$[0].sentAt").exists());
	}

	/** A document with nothing scheduled says so with an empty list, not a 404. */
	@Test
	void aDocumentWithNoRemindersAnswersAnEmptyList() throws Exception {
		Garage garage = givenDocument("empty@example.com", "VIN0000000000EMPTY");

		mockMvc.perform(get(path(garage))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + garage.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	/**
	 * The distinction the ownership check exists to make. Without it this would
	 * answer an empty list - the same answer as a document that genuinely has no
	 * reminders, and the same answer as a UUID typed wrongly. Section 15: knowing
	 * an identifier is never enough.
	 */
	@Test
	void anotherAccountsDocumentIsNotFoundRatherThanEmpty() throws Exception {
		Garage mine = givenDocument("owner@example.com", "VIN0000000000OWNER");
		Garage theirs = givenDocument("stranger@example.com", "VIN000000STRANGER1");

		givenReminder(mine.documentId(), 30, Instant.now().plusSeconds(60), ReminderStatus.PENDING);

		mockMvc.perform(get(path(mine))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + theirs.token()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("DOCUMENT_NOT_FOUND"));
	}

	/** The vehicle in the path is matched too, not merely carried. */
	@Test
	void aDocumentAskedForUnderTheWrongVehicleIsNotFound() throws Exception {
		Garage first = givenDocument("twocars@example.com", "VIN00000TWOCARS001");
		Garage second = givenDocument("othercar@example.com", "VIN00000OTHERCAR01");

		mockMvc.perform(get("/api/v1/vehicles/" + second.vehicleId() + "/documents/"
						+ first.documentId() + "/reminders")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + first.token()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("DOCUMENT_NOT_FOUND"));
	}

	@Test
	void aRequestWithoutATokenIsRefused() throws Exception {
		Garage garage = givenDocument("anonymous@example.com", "VIN0000ANONYMOUS01");

		mockMvc.perform(get(path(garage)))
				.andExpect(status().isUnauthorized());
	}
}