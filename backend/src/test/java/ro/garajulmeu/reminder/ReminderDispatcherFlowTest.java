package ro.garajulmeu.reminder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

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
import ro.garajulmeu.push.PushTokenRejectedException;
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

	/**
	 * Needed only to clear it. A device removed by {@code deleteAll} takes its
	 * delivery rows with it through V10's cascade, and rows the database removes
	 * are invisible to a persistence context still holding them.
	 */
	@PersistenceContext
	private EntityManager entityManager;

	/** Unused. Present only so this class shares AuthFlowTest's context. */
	@MockitoBean
	private EmailProvider emailProvider;

	/**
	 * Remembers what it was asked to send, and fails the tokens it was given -
	 * <strong>in the two different ways a real provider fails.</strong>
	 *
	 * <p>{@code refused} is an outage, a throttle, a payload the platform would
	 * not take: it may work next time, so the dispatcher retries. {@code rejected}
	 * is FCM's {@code UNREGISTERED} - a fact about the handset rather than about
	 * the moment - and retrying it is waste. A test provider that could only
	 * express one of them could not tell the two behaviours apart.
	 */
	private static final class Recorder implements PushNotificationProvider {

		private final List<String> reached = new ArrayList<>();
		private final List<PushNotification> notifications = new ArrayList<>();
		private final Set<String> refused;
		private final Set<String> rejected;

		Recorder(String... refused) {
			this(Set.of(refused), Set.of());
		}

		private Recorder(Set<String> refused, Set<String> rejected) {
			this.refused = refused;
			this.rejected = rejected;
		}

		/** Reports these tokens as permanently dead, the way FCM reports an uninstall. */
		static Recorder rejecting(String... tokens) {
			return new Recorder(Set.of(), Set.of(tokens));
		}

		@Override
		public void send(String pushToken, PushNotification notification) {
			if (rejected.contains(pushToken)) {
				throw new PushTokenRejectedException("token is no longer registered");
			}
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

	/**
	 * The distinction the whole of decision 3 rests on: a dead handset is not a
	 * slow one.
	 *
	 * <p>The attempt count is the assertion that matters. Before the rejection was
	 * given its own meaning, this account's only phone would have been attempted
	 * three times, once a minute, for every reminder it ever received - and the
	 * reminder would have ended FAILED, which is not true either. Nobody was
	 * reachable, which is the "no device" case arriving one pass late, and section
	 * 18's answer to that is that the work is finished.
	 */
	@Test
	void aPermanentlyRejectedTokenRemovesTheDeviceInsteadOfRetryingIt() {
		Fixture fixture = givenDueReminder("uninstalled@example.com", "VIN000UNINSTALLED0");
		givenDevice(fixture.userId(), "token-dead", true);

		ReminderDispatcher dispatcher = dispatcherWith(Recorder.rejecting("token-dead"));

		dispatcher.dispatch(claimOwn(dispatcher, fixture));

		Reminder reminder = reload(fixture);
		assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.SENT);
		assertThat(reminder.getAttemptCount()).isEqualTo(1);
		assertThat(reminder.getSentAt()).isNotNull();
		assertThat(reminder.getLastErrorCode()).isEqualTo("PUSH_DELIVERY_FAILED");

		// Cleared before reading, because a row removed by the database is
		// invisible to a persistence context that still holds it - the mistake
		// 12.3b paid for, where findById answered out of the first-level cache and
		// reported a vehicle that was already gone.
		entityManager.clear();

		assertThat(deviceRepository.findByUserIdAndNotificationsEnabledTrue(fixture.userId()))
				.as("devices left after the provider rejected the only one")
				.isEmpty();
	}

	/**
	 * The consequence of that removal, asserted rather than left in a comment.
	 *
	 * <p>V10 declares {@code ON DELETE CASCADE} from {@code notification_deliveries}
	 * to {@code user_devices}, so deleting the device takes the very row that
	 * recorded why it was deleted. That was weighed and accepted - it is history
	 * about a handset that no longer exists, and the reminder's own code and the
	 * dispatcher's log line both survive - but somebody will one day go looking
	 * for that row, and this test is what tells them it was a decision.
	 */
	@Test
	void removingARejectedDeviceTakesItsDeliveryRowsWithIt() {
		Fixture fixture = givenDueReminder("cascade@example.com", "VIN00000000CASCADE");
		givenDevice(fixture.userId(), "token-dead", true);

		ReminderDispatcher dispatcher = dispatcherWith(Recorder.rejecting("token-dead"));

		dispatcher.dispatch(claimOwn(dispatcher, fixture));
		entityManager.clear();

		assertThat(deliveryRepository.findByReminderId(fixture.reminderId()))
				.as("delivery rows surviving the device they belonged to")
				.isEmpty();
	}

	/**
	 * One dead phone must not hold back a reminder that reached a living one.
	 *
	 * <p>Under the old rule this ended PENDING and was retried twice more, each
	 * pass reaching nobody new: the good device was already SENT and skipped by the
	 * unique index, and the dead one was never coming back. The reminder now
	 * finishes on the first pass, and the garage keeps the phone that works.
	 */
	@Test
	void aRejectedDeviceDoesNotHoldBackAReminderThatReachedAnother() {
		Fixture fixture = givenDueReminder("mixed@example.com", "VIN0000000000MIXED");
		givenDevice(fixture.userId(), "token-good", true);
		givenDevice(fixture.userId(), "token-dead", true);

		Recorder provider = Recorder.rejecting("token-dead");
		ReminderDispatcher dispatcher = dispatcherWith(provider);

		dispatcher.dispatch(claimOwn(dispatcher, fixture));

		assertThat(provider.reached).containsExactly("token-good");

		Reminder reminder = reload(fixture);
		assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.SENT);
		assertThat(reminder.getAttemptCount()).isEqualTo(1);
		assertThat(reminder.getLastErrorCode()).isEqualTo("PUSH_DELIVERY_FAILED");

		entityManager.clear();

		assertThat(deviceRepository.findByUserIdAndNotificationsEnabledTrue(fixture.userId()))
				.as("devices left after one of two was rejected")
				.singleElement()
				.matches(device -> device.getPushToken().equals("token-good"),
						"the phone that answered is still registered");
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