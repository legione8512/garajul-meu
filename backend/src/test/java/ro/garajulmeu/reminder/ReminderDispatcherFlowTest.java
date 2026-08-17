package ro.garajulmeu.reminder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import ro.garajulmeu.TestcontainersConfiguration;
import ro.garajulmeu.email.EmailProvider;
import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;
import ro.garajulmeu.notification.DeliveryStatus;
import ro.garajulmeu.notification.NotificationDelivery;
import ro.garajulmeu.notification.NotificationDeliveryRepository;
import ro.garajulmeu.push.DevicePlatform;
import ro.garajulmeu.push.PushNotification;
import ro.garajulmeu.push.PushNotificationProvider;
import ro.garajulmeu.push.UserDevice;
import ro.garajulmeu.push.UserDeviceRepository;
import ro.garajulmeu.registrationcertificate.RegistrationCertificate;
import ro.garajulmeu.registrationcertificate.RegistrationCertificateRepository;
import ro.garajulmeu.user.User;
import ro.garajulmeu.user.UserRepository;
import ro.garajulmeu.vehicle.Vehicle;
import ro.garajulmeu.vehicle.VehicleRepository;
import ro.garajulmeu.vehicledocument.DocumentType;
import ro.garajulmeu.vehicledocument.VehicleDocument;
import ro.garajulmeu.vehicledocument.VehicleDocumentRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Annotations identical to AuthFlowTest, including the unused EmailProvider mock
 * and the MockMvc this class never touches, so the cached context is reused and
 * no further container starts.
 *
 * <p><strong>The dispatcher is built by hand rather than injected.</strong> Two
 * reasons, and the second is the important one. The push provider has to be
 * controllable, and a {@code @MockitoBean} for it would change the context
 * configuration and start a fifth container - the whole project has been
 * arranged to avoid exactly that. And building it here means the retry limit is
 * a value in this file rather than a property, so a test about the third attempt
 * says "3" where it is read.
 *
 * <p>What this costs, stated plainly: a hand-built instance has no Spring proxy,
 * so the three {@code @Transactional} boundaries do not exist here - everything
 * joins the test's own transaction and rolls back. These tests verify the state
 * machine, not the commit boundaries. The boundaries are argued for in the
 * dispatcher's own documentation and are the one thing here no test asserts.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class ReminderDispatcherFlowTest {

	private static final int MAX_ATTEMPTS = 3;

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
	private NotificationDeliveryRepository deliveryRepository;

	@Autowired
	private UserDeviceRepository deviceRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private Clock clock;

	/** Unused. Present only so this class shares AuthFlowTest's context. */
	@MockitoBean
	private EmailProvider emailProvider;

	/** Remembers what it was asked to send, and refuses the tokens it was given. */
	private static final class Recorder implements PushNotificationProvider {

		private final List<String> reached = new ArrayList<>();
		private final List<PushNotification> notifications = new ArrayList<>();
		private final Set<String> refused;

		Recorder(String... refused) {
			this.refused = Set.of(refused);
		}

		@Override
		public void send(String pushToken, PushNotification notification) {
			if (refused.contains(pushToken)) {
				throw new ApiException(ErrorCode.INTERNAL_ERROR);
			}
			reached.add(pushToken);
			notifications.add(notification);
		}
	}

	private record Fixture(UUID userId, UUID reminderId) {
	}

	private ReminderDispatcher dispatcherWith(PushNotificationProvider provider) {
		return new ReminderDispatcher(reminderRepository, deliveryRepository, deviceRepository,
				provider, new ReminderProperties(false, 60, 50, MAX_ATTEMPTS, 10), clock);
	}

	private Fixture givenReminder(String email, String vin, Instant scheduledAt) {
		User user = new User("Marius Robert", email,
				passwordEncoder.encode("a-long-enough-password"));
		user.setEmailVerifiedAt(Instant.now());
		UUID userId = userRepository.saveAndFlush(user).getId();

		Vehicle vehicle = new Vehicle(userId);
		vehicle.setDisplayName("Logan");
		UUID vehicleId = vehicleRepository.saveAndFlush(vehicle).getId();

		certificateRepository.saveAndFlush(new RegistrationCertificate(vehicleId, userId,
				"B123ABC", "Dacia", "Logan", vin));

		VehicleDocument document = new VehicleDocument(vehicleId, DocumentType.RCA,
				LocalDate.now().plusDays(7));
		UUID documentId = documentRepository.saveAndFlush(document).getId();

		Reminder reminder = new Reminder(documentId, 7, scheduledAt);
		return new Fixture(userId, reminderRepository.saveAndFlush(reminder).getId());
	}

	private Fixture givenDueReminder(String email, String vin) {
		return givenReminder(email, vin, Instant.now().minusSeconds(60));
	}

	private void givenDevice(UUID userId, String token, boolean enabled) {
		UserDevice device = new UserDevice(userId, DevicePlatform.ANDROID, token);
		device.setNotificationsEnabled(enabled);
		deviceRepository.saveAndFlush(device);
	}

	/**
	 * A pass claims whatever the database holds, so a test asserts about its own
	 * row rather than about the size of the batch. Everything here rolls back -
	 * but a test that assumed it was alone would start failing the day one does
	 * not.
	 */
	private DueReminder claimOwn(ReminderDispatcher dispatcher, Fixture fixture) {
		return dispatcher.claimDue().stream()
				.filter(due -> due.reminderId().equals(fixture.reminderId()))
				.findFirst()
				.orElseThrow();
	}

	private Reminder reload(Fixture fixture) {
		return reminderRepository.findById(fixture.reminderId()).orElseThrow();
	}

	/**
	 * The case that is every case in V1 web: section 18 makes push native-only and
	 * the applications do not exist yet. The reminder is finished, and the
	 * complete absence of delivery rows is what says nothing was sent.
	 */
	@Test
	void anAccountWithNoDeviceFinishesTheReminderAndDeliversNothing() {
		Fixture fixture = givenDueReminder("nodevice@example.com", "VIN0000000NODEVICE");
		Recorder provider = new Recorder();
		ReminderDispatcher dispatcher = dispatcherWith(provider);

		dispatcher.dispatch(claimOwn(dispatcher, fixture));

		Reminder reminder = reload(fixture);
		assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.SENT);
		assertThat(reminder.getSentAt()).isNotNull();
		assertThat(reminder.getAttemptCount()).isEqualTo(1);
		assertThat(deliveryRepository.findByReminderId(fixture.reminderId())).isEmpty();
		assertThat(provider.reached).isEmpty();
	}

	@Test
	void everyEnabledDeviceIsReachedAndEachGetsItsOwnDeliveryRow() {
		Fixture fixture = givenDueReminder("twophones@example.com", "VIN000000TWOPHONE");
		givenDevice(fixture.userId(), "token-one", true);
		givenDevice(fixture.userId(), "token-two", true);

		Recorder provider = new Recorder();
		ReminderDispatcher dispatcher = dispatcherWith(provider);

		dispatcher.dispatch(claimOwn(dispatcher, fixture));

		assertThat(provider.reached).containsExactlyInAnyOrder("token-one", "token-two");
		assertThat(deliveryRepository.findByReminderId(fixture.reminderId()))
				.hasSize(2)
				.allMatch(delivery -> delivery.getStatus() == DeliveryStatus.SENT)
				.allMatch(delivery -> delivery.getAttemptCount() == 1);
		assertThat(reload(fixture).getStatus()).isEqualTo(ReminderStatus.SENT);
	}

	/**
	 * The wording is composed from the account's language and the offset, and the
	 * payload carries identifiers only. Asserted here as well as in
	 * ReminderMessageTest because this is the path a real notification takes, and
	 * a projection that fetched the wrong column would produce a perfectly
	 * well-formed sentence about the wrong car.
	 */
	@Test
	void theMessageNamesTheVehicleAndCarriesTheDeepLink() {
		Fixture fixture = givenDueReminder("wording@example.com", "VIN00000000WORDING");
		givenDevice(fixture.userId(), "token-wording", true);

		Recorder provider = new Recorder();
		ReminderDispatcher dispatcher = dispatcherWith(provider);

		DueReminder due = claimOwn(dispatcher, fixture);
		dispatcher.dispatch(due);

		assertThat(provider.notifications).hasSize(1);
		PushNotification notification = provider.notifications.get(0);
		assertThat(notification.title()).isEqualTo("RCA expiră în 7 zile");
		assertThat(notification.body()).startsWith("Logan — până la ");
		assertThat(notification.data()).containsOnlyKeys("vehicleId", "documentId", "type");
		assertThat(notification.data().get("documentId")).isEqualTo(due.documentId().toString());
	}

	/** One phone silenced is not the account silenced. Section 10.7's own switch. */
	@Test
	void aDeviceWithNotificationsTurnedOffIsNotReached() {
		Fixture fixture = givenDueReminder("silenced@example.com", "VIN0000000SILENCED");
		givenDevice(fixture.userId(), "token-on", true);
		givenDevice(fixture.userId(), "token-off", false);

		Recorder provider = new Recorder();
		ReminderDispatcher dispatcher = dispatcherWith(provider);

		dispatcher.dispatch(claimOwn(dispatcher, fixture));

		assertThat(provider.reached).containsExactly("token-on");
		assertThat(deliveryRepository.findByReminderId(fixture.reminderId())).hasSize(1);
	}

	/**
	 * A refusal leaves the reminder where the next pass will find it, with
	 * scheduled_at untouched so it is still overdue.
	 */
	@Test
	void aProviderRefusalLeavesTheReminderPendingWithItsCodeRecorded() {
		Fixture fixture = givenDueReminder("refused@example.com", "VIN00000000REFUSED");
		givenDevice(fixture.userId(), "token-bad", true);

		ReminderDispatcher dispatcher = dispatcherWith(new Recorder("token-bad"));
		Instant scheduledBefore = reload(fixture).getScheduledAt();

		dispatcher.dispatch(claimOwn(dispatcher, fixture));

		Reminder reminder = reload(fixture);
		assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.PENDING);
		assertThat(reminder.getAttemptCount()).isEqualTo(1);
		assertThat(reminder.getScheduledAt()).isEqualTo(scheduledBefore);
		assertThat(reminder.getSentAt()).isNull();

		NotificationDelivery delivery = deliveryRepository
				.findByReminderId(fixture.reminderId()).get(0);
		assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.FAILED);
		assertThat(delivery.getLastErrorCode()).isEqualTo("INTERNAL_ERROR");
	}

	@Test
	void aReminderThatReachesNothingIsFailedAfterTheLastAttempt() {
		Fixture fixture = givenDueReminder("hopeless@example.com", "VIN000000HOPELESS0");
		givenDevice(fixture.userId(), "token-bad", true);

		ReminderDispatcher dispatcher = dispatcherWith(new Recorder("token-bad"));

		for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
			dispatcher.dispatch(claimOwn(dispatcher, fixture));
		}

		Reminder reminder = reload(fixture);
		assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.FAILED);
		assertThat(reminder.getAttemptCount()).isEqualTo(MAX_ATTEMPTS);
		assertThat(reminder.getLastErrorCode()).isEqualTo("PUSH_DELIVERY_FAILED");
		assertThat(deliveryRepository.findByReminderId(fixture.reminderId()).get(0)
				.getAttemptCount()).isEqualTo(MAX_ATTEMPTS);
	}

	/**
	 * The point of the unique index on (reminder, device): a retry finds the row
	 * it wrote last time. The phone that already has the notification is attempted
	 * once and never again, while the broken one is attempted three times - and
	 * because somebody was told, the reminder ends SENT rather than FAILED, with
	 * the code left on so the partial is visible.
	 */
	@Test
	void aRetryReachesOnlyTheDeviceThatFailed() {
		Fixture fixture = givenDueReminder("partial@example.com", "VIN0000000PARTIAL0");
		givenDevice(fixture.userId(), "token-good", true);
		givenDevice(fixture.userId(), "token-bad", true);

		ReminderDispatcher first = dispatcherWith(new Recorder("token-bad"));
		first.dispatch(claimOwn(first, fixture));
		assertThat(reload(fixture).getStatus()).isEqualTo(ReminderStatus.PENDING);

		Recorder second = new Recorder("token-bad");
		ReminderDispatcher retry = dispatcherWith(second);
		retry.dispatch(claimOwn(retry, fixture));

		assertThat(second.reached).isEmpty();
		assertThat(reload(fixture).getStatus()).isEqualTo(ReminderStatus.PENDING);

		Recorder third = new Recorder("token-bad");
		ReminderDispatcher last = dispatcherWith(third);
		last.dispatch(claimOwn(last, fixture));

		Reminder reminder = reload(fixture);
		assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.SENT);
		assertThat(reminder.getLastErrorCode()).isEqualTo("PUSH_DELIVERY_FAILED");

		assertThat(deliveryRepository.findByReminderId(fixture.reminderId()))
				.hasSize(2)
				.filteredOn(delivery -> delivery.getStatus() == DeliveryStatus.SENT)
				.singleElement()
				.matches(delivery -> delivery.getAttemptCount() == 1,
						"reached once and never disturbed again");
	}

	/** Tomorrow's reminder is not today's work. */
	@Test
	void aReminderNotYetDueIsNotClaimed() {
		Fixture fixture = givenReminder("future@example.com", "VIN000000000FUTURE",
				Instant.now().plus(1, ChronoUnit.DAYS));

		ReminderDispatcher dispatcher = dispatcherWith(new Recorder());

		assertThat(dispatcher.claimDue())
				.noneMatch(due -> due.reminderId().equals(fixture.reminderId()));
		assertThat(reload(fixture).getStatus()).isEqualTo(ReminderStatus.PENDING);
	}

	/**
	 * Section 12 requires that a short restart lose no reminder, and PROCESSING is
	 * a state its own query - PENDING and due - can never reach. Without the
	 * release, this row is invisible for ever.
	 */
	@Test
	void aReminderAbandonedInProcessingIsReleasedAndTakenAgain() {
		Fixture fixture = givenDueReminder("stalled@example.com", "VIN0000000STALLED0");

		Reminder crashed = reload(fixture);
		crashed.setStatus(ReminderStatus.PROCESSING);
		crashed.setLastAttemptAt(Instant.now().minus(30, ChronoUnit.MINUTES));
		reminderRepository.saveAndFlush(crashed);

		ReminderDispatcher dispatcher = dispatcherWith(new Recorder());

		assertThat(dispatcher.claimDue())
				.noneMatch(due -> due.reminderId().equals(fixture.reminderId()));

		assertThat(dispatcher.releaseStalled()).isPositive();
		assertThat(reload(fixture).getStatus()).isEqualTo(ReminderStatus.PENDING);
		assertThat(claimOwn(dispatcher, fixture)).isNotNull();
	}
}