package ro.garajulmeu.vehiclepayment;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import ro.garajulmeu.TestcontainersConfiguration;
import ro.garajulmeu.email.EmailProvider;
import ro.garajulmeu.notification.NotificationPreferences;
import ro.garajulmeu.notification.NotificationPreferencesRepository;
import ro.garajulmeu.registrationcertificate.RegistrationCertificate;
import ro.garajulmeu.registrationcertificate.RegistrationCertificateRepository;
import ro.garajulmeu.reminder.Reminder;
import ro.garajulmeu.reminder.ReminderRepository;
import ro.garajulmeu.reminder.ReminderService;
import ro.garajulmeu.reminder.ReminderStatus;
import ro.garajulmeu.security.AccessTokenService;
import ro.garajulmeu.user.User;
import ro.garajulmeu.user.UserRepository;
import ro.garajulmeu.vehicle.Vehicle;
import ro.garajulmeu.vehicle.VehicleRepository;
import ro.garajulmeu.vehicledocument.DocumentCoverage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 1.1's recurring payments through the API, and the reminders and dashboard
 * lines that follow them.
 *
 * <p>Annotations identical to AuthFlowTest, including the unused EmailProvider
 * mock, so the cached context is reused and no further container starts.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class VehiclePaymentFlowTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private VehicleRepository vehicleRepository;

	@Autowired
	private RegistrationCertificateRepository certificateRepository;

	@Autowired
	private VehiclePaymentRepository paymentRepository;

	@Autowired
	private ReminderRepository reminderRepository;

	@Autowired
	private ReminderService reminderService;

	@Autowired
	private NotificationPreferencesRepository preferencesRepository;

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

	/** The service's own "today", never a bare LocalDate.now(). */
	private LocalDate today() {
		return DocumentCoverage.todayFor(clock, ZoneId.of("Europe/Bucharest"));
	}

	private static String path(UUID vehicleId) {
		return "/api/v1/vehicles/" + vehicleId + "/payments";
	}

	private ResultActions send(Account account, String method, String path, String body)
			throws Exception {
		var request = switch (method) {
			case "POST" -> post(path);
			case "PATCH" -> patch(path);
			default -> throw new IllegalArgumentException(method);
		};
		return mockMvc.perform(request
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private static String byCount(String kind, LocalDate first, String frequency, int count,
			String leads) {
		return """
				{"kind":"%s","firstDueDate":"%s","frequency":"%s","instalmentCount":%d%s}
				""".formatted(kind, first, frequency, count,
				leads == null ? "" : ",\"remindDaysBefore\":" + leads);
	}

	private UUID created(ResultActions result) throws Exception {
		String json = result.andExpect(status().isCreated()).andReturn().getResponse()
				.getContentAsString();
		return UUID.fromString(JsonPath.read(json, "$.id"));
	}

	private List<Reminder> pendingOf(UUID paymentId) {
		return reminderRepository.findByVehiclePaymentIdOrderByScheduledAt(paymentId).stream()
				.filter(reminder -> reminder.getStatus() == ReminderStatus.PENDING)
				.toList();
	}

	@Test
	void aPaymentGivenByCountComesBackWithItsEndAndNextInstalment() throws Exception {
		Account account = givenAccount("payments@example.com");
		UUID vehicleId = givenVehicle(account.id(), "VF1PAYMENT0000001");
		LocalDate first = today().plusDays(10);

		send(account, "POST", path(vehicleId), byCount("loan", first, "monthly", 36, null))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.kind").value("LOAN"))
				.andExpect(jsonPath("$.frequency").value("MONTHLY"))
				.andExpect(jsonPath("$.lastDueDate").value(first.plusMonths(35).toString()))
				.andExpect(jsonPath("$.instalmentCount").value(36))
				.andExpect(jsonPath("$.remindDaysBefore[0]").value(3))
				.andExpect(jsonPath("$.remindDaysBefore[1]").value(1))
				.andExpect(jsonPath("$.nextDueDate").value(first.toString()))
				.andExpect(jsonPath("$.nextInstalment").value(1))
				.andExpect(jsonPath("$.daysUntilNext").value(10));

		mockMvc.perform(get(path(vehicleId))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	/** A contract's final date is stored as the last instalment's own date. */
	@Test
	void aPaymentGivenByLastDateEndsOnTheInstalmentBeforeIt() throws Exception {
		Account account = givenAccount("bydate@example.com");
		UUID vehicleId = givenVehicle(account.id(), "VF1PAYMENT0000002");
		LocalDate first = today().plusDays(10);

		send(account, "POST", path(vehicleId), """
				{"kind":"CASCO","firstDueDate":"%s","frequency":"QUARTERLY","lastDueDate":"%s"}
				""".formatted(first, first.plusMonths(7)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.lastDueDate").value(first.plusMonths(6).toString()))
				.andExpect(jsonPath("$.instalmentCount").value(3));
	}

	/** Two leads for each of three instalments, all still ahead. */
	@Test
	void savingAPaymentSchedulesAReminderForEveryInstalmentAndLead() throws Exception {
		Account account = givenAccount("scheduled@example.com");
		UUID vehicleId = givenVehicle(account.id(), "VF1PAYMENT0000003");

		UUID paymentId = created(send(account, "POST", path(vehicleId),
				byCount("LOAN", today().plusDays(10), "MONTHLY", 3, null)));

		List<Reminder> pending = pendingOf(paymentId);
		assertThat(pending).hasSize(6);
		assertThat(pending).extracting(Reminder::getOffsetDays).containsOnly(3, 1);
		assertThat(pending).extracting(Reminder::getVehicleDocumentId).containsOnlyNulls();
		assertThat(pending).extracting(Reminder::getDueDate).containsOnly(
				today().plusDays(10), today().plusDays(10).plusMonths(1),
				today().plusDays(10).plusMonths(2));
	}

	@Test
	void theChosenLeadsAreStoredLargestFirstAndDuplicatesCollapse() throws Exception {
		Account account = givenAccount("leads@example.com");
		UUID vehicleId = givenVehicle(account.id(), "VF1PAYMENT0000004");

		send(account, "POST", path(vehicleId),
				byCount("LOAN", today().plusDays(10), "MONTHLY", 1, "[0,7,7,1]"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.remindDaysBefore.length()").value(3))
				.andExpect(jsonPath("$.remindDaysBefore[0]").value(7))
				.andExpect(jsonPath("$.remindDaysBefore[1]").value(1))
				.andExpect(jsonPath("$.remindDaysBefore[2]").value(0));
	}

	@Test
	void anEmptyChoiceOfLeadsIsTheDefault() throws Exception {
		Account account = givenAccount("emptyleads@example.com");
		UUID vehicleId = givenVehicle(account.id(), "VF1PAYMENT0000005");

		send(account, "POST", path(vehicleId),
				byCount("LOAN", today().plusDays(10), "MONTHLY", 1, "[]"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.remindDaysBefore.length()").value(2));
	}

	@Test
	void aScheduleWithNoEndTwoEndsOrTooManyInstalmentsIsRefused() throws Exception {
		Account account = givenAccount("badschedule@example.com");
		UUID vehicleId = givenVehicle(account.id(), "VF1PAYMENT0000006");
		LocalDate first = today().plusDays(10);

		send(account, "POST", path(vehicleId), """
				{"kind":"LOAN","firstDueDate":"%s","frequency":"MONTHLY"}
				""".formatted(first))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("PAYMENT_INVALID_SCHEDULE"));

		send(account, "POST", path(vehicleId), """
				{"kind":"LOAN","firstDueDate":"%s","frequency":"MONTHLY","instalmentCount":3,"lastDueDate":"%s"}
				""".formatted(first, first.plusMonths(2)))
				.andExpect(jsonPath("$.code").value("PAYMENT_INVALID_SCHEDULE"));

		send(account, "POST", path(vehicleId), byCount("LOAN", first, "MONTHLY", 121, null))
				.andExpect(jsonPath("$.code").value("PAYMENT_INVALID_SCHEDULE"));

		send(account, "POST", path(vehicleId), byCount("LOAN", first, "MONTHLY", 0, null))
				.andExpect(jsonPath("$.code").value("PAYMENT_INVALID_SCHEDULE"));

		send(account, "POST", path(vehicleId), """
				{"kind":"LOAN","firstDueDate":"%s","frequency":"MONTHLY","lastDueDate":"%s"}
				""".formatted(first, first.minusDays(1)))
				.andExpect(jsonPath("$.code").value("PAYMENT_INVALID_SCHEDULE"));

		assertThat(paymentRepository.ofVehicle(vehicleId, account.id())).isEmpty();
	}

	@Test
	void anUnknownKindFrequencyOrLeadIsAValidationError() throws Exception {
		Account account = givenAccount("unknown@example.com");
		UUID vehicleId = givenVehicle(account.id(), "VF1PAYMENT0000007");
		LocalDate first = today().plusDays(10);

		send(account, "POST", path(vehicleId), byCount("MORTGAGE", first, "MONTHLY", 3, null))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		send(account, "POST", path(vehicleId), byCount("LOAN", first, "WEEKLY", 3, null))
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		send(account, "POST", path(vehicleId), byCount("LOAN", first, "MONTHLY", 3, "[5]"))
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	/**
	 * A correction replaces the schedule and its reminders: the old PENDING rows
	 * are cancelled, not deleted, and a new set stands in their place.
	 */
	@Test
	void aCorrectionCancelsTheOldRemindersAndSchedulesTheNewSeries() throws Exception {
		Account account = givenAccount("correct@example.com");
		UUID vehicleId = givenVehicle(account.id(), "VF1PAYMENT0000008");

		UUID paymentId = created(send(account, "POST", path(vehicleId),
				byCount("LOAN", today().plusDays(10), "MONTHLY", 3, null)));

		send(account, "PATCH", path(vehicleId) + "/" + paymentId,
				byCount("CASCO", today().plusDays(20), "MONTHLY", 2, "[7]"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.kind").value("CASCO"))
				.andExpect(jsonPath("$.instalmentCount").value(2));

		List<Reminder> all = reminderRepository.findByVehiclePaymentIdOrderByScheduledAt(paymentId);
		assertThat(all).filteredOn(reminder -> reminder.getStatus() == ReminderStatus.CANCELLED)
				.hasSize(6);
		assertThat(pendingOf(paymentId)).hasSize(2)
				.extracting(Reminder::getOffsetDays).containsOnly(7);
	}

	/** Everything is checked before anything is set, so a refusal changes nothing. */
	@Test
	void aRefusedCorrectionLeavesThePaymentAsItWas() throws Exception {
		Account account = givenAccount("refused@example.com");
		UUID vehicleId = givenVehicle(account.id(), "VF1PAYMENT0000009");
		LocalDate first = today().plusDays(10);

		UUID paymentId = created(send(account, "POST", path(vehicleId),
				byCount("LOAN", first, "MONTHLY", 3, null)));

		send(account, "PATCH", path(vehicleId) + "/" + paymentId,
				byCount("CASCO", first, "MONTHLY", 3, "[5]"))
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		VehiclePayment stored = paymentRepository.findById(paymentId).orElseThrow();
		assertThat(stored.getKind()).isEqualTo(PaymentKind.LOAN);
		assertThat(pendingOf(paymentId)).hasSize(6);
	}

	@Test
	void deletingAPaymentTakesItsRemindersWithIt() throws Exception {
		Account account = givenAccount("deleted@example.com");
		UUID vehicleId = givenVehicle(account.id(), "VF1PAYMENT0000010");

		UUID paymentId = created(send(account, "POST", path(vehicleId),
				byCount("LOAN", today().plusDays(10), "MONTHLY", 3, null)));

		mockMvc.perform(delete(path(vehicleId) + "/" + paymentId)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token()))
				.andExpect(status().isNoContent());

		assertThat(paymentRepository.findById(paymentId)).isEmpty();
		assertThat(reminderRepository.findByVehiclePaymentIdOrderByScheduledAt(paymentId)).isEmpty();
	}

	/** Knowing a vehicle's or a payment's UUID is never enough. */
	@Test
	void anotherAccountsVehicleAndPaymentAreNotFound() throws Exception {
		Account owner = givenAccount("owner@example.com");
		Account stranger = givenAccount("stranger@example.com");
		UUID vehicleId = givenVehicle(owner.id(), "VF1PAYMENT0000011");
		UUID strangersVehicle = givenVehicle(stranger.id(), "VF1PAYMENT0000012");

		UUID paymentId = created(send(owner, "POST", path(vehicleId),
				byCount("LOAN", today().plusDays(10), "MONTHLY", 3, null)));

		send(stranger, "POST", path(vehicleId), byCount("LOAN", today(), "MONTHLY", 3, null))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("VEHICLE_NOT_FOUND"));

		send(stranger, "PATCH", path(strangersVehicle) + "/" + paymentId,
				byCount("LOAN", today(), "MONTHLY", 3, null))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PAYMENT_NOT_FOUND"));

		mockMvc.perform(delete(path(strangersVehicle) + "/" + paymentId)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + stranger.token()))
				.andExpect(jsonPath("$.code").value("PAYMENT_NOT_FOUND"));

		assertThat(paymentRepository.findById(paymentId)).isPresent();
	}

	/**
	 * The account-level switch silences instalments as it silences documents,
	 * and a preferences change reconciles the garage's payments too.
	 */
	@Test
	void theAccountSwitchSilencesInstalmentsAndTheGarageReconcileReachesThem() throws Exception {
		Account account = givenAccount("silenced@example.com");
		UUID vehicleId = givenVehicle(account.id(), "VF1PAYMENT0000013");

		UUID paymentId = created(send(account, "POST", path(vehicleId),
				byCount("LOAN", today().plusDays(10), "MONTHLY", 3, null)));
		assertThat(pendingOf(paymentId)).hasSize(6);

		NotificationPreferences preferences = preferencesRepository.findByUserId(account.id())
				.orElseGet(() -> NotificationPreferences.defaultsFor(account.id()));
		preferences.setNotificationsEnabled(false);
		preferencesRepository.saveAndFlush(preferences);

		reminderService.reconcileGarage(account.id());

		assertThat(pendingOf(paymentId)).isEmpty();
	}

	/**
	 * On Acasă, 1.1: an instalment due within seven days, today included, appears
	 * on its vehicle's card; one further off does not.
	 */
	@Test
	void theDashboardShowsOnlyInstalmentsDueWithinSevenDays() throws Exception {
		Account account = givenAccount("dashboard@example.com");
		UUID vehicleId = givenVehicle(account.id(), "VF1PAYMENT0000014");

		created(send(account, "POST", path(vehicleId),
				byCount("CASCO", today().plusDays(7), "MONTHLY", 3, null)));
		created(send(account, "POST", path(vehicleId),
				byCount("LOAN", today().plusDays(8), "MONTHLY", 3, null)));
		// A series that started a month ago, so its second instalment is today -
		// except on a 29th to 31st whose previous month is shorter, where the
		// anchor rule cannot land on today and the series simply starts today.
		LocalDate started = today().minusMonths(1).plusMonths(1).equals(today())
				? today().minusMonths(1) : today();
		int todaysInstalment = started.equals(today()) ? 1 : 2;
		created(send(account, "POST", path(vehicleId),
				byCount("LOAN", started, "MONTHLY", 3, null)));

		mockMvc.perform(get("/api/v1/dashboard")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.vehicles[0].payments.length()").value(2))
				.andExpect(jsonPath("$.vehicles[0].payments[0].kind").value("LOAN"))
				.andExpect(jsonPath("$.vehicles[0].payments[0].daysRemaining").value(0))
				.andExpect(jsonPath("$.vehicles[0].payments[0].instalment").value(todaysInstalment))
				.andExpect(jsonPath("$.vehicles[0].payments[0].instalments").value(3))
				.andExpect(jsonPath("$.vehicles[0].payments[1].kind").value("CASCO"))
				.andExpect(jsonPath("$.vehicles[0].payments[1].daysRemaining").value(7));
	}
}
