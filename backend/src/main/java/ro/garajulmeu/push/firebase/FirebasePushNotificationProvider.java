package ro.garajulmeu.push.firebase;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;

import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;
import ro.garajulmeu.push.PushNotification;
import ro.garajulmeu.push.PushNotificationProvider;
import ro.garajulmeu.push.PushTokenRejectedException;

/**
 * Delivers for real, through Firebase Cloud Messaging. Specification sections
 * 18 and 36.
 *
 * <p>
 * Selected by an explicit property like the other two seams, so production
 * cannot fall back to it or away from it silently.
 *
 * <p>
 * <strong>The whole of this class is the error mapping.</strong> Sending is
 * four lines; deciding what a refusal <em>means</em> is the part the interface
 * warns about, because getting it wrong is expensive in both directions - a
 * dead handset reported as transient buys three retries a minute for ever, and
 * an outage reported as a dead handset unregisters a phone that was working ten
 * seconds ago.
 */
@Component
@ConditionalOnProperty(name = "garajul-meu.push.provider", havingValue = "firebase")
class FirebasePushNotificationProvider implements PushNotificationProvider, AutoCloseable {

	private static final Logger log = LoggerFactory.getLogger(FirebasePushNotificationProvider.class);

	/**
	 * A named app rather than the default one. {@code FirebaseApp} is a static
	 * registry, so the default instance is a global this class would be claiming on
	 * everybody's behalf; a name keeps it to ours and makes it findable again when
	 * a second Spring context appears in a test run.
	 */
	private static final String APP_NAME = "garajul-meu";

	/**
	 * The two answers that describe the <em>device</em> rather than the moment.
	 *
	 * <p>
	 * {@code UNREGISTERED} is a token that was valid and is not any more - the
	 * application was uninstalled, or the token was rotated and this one retired.
	 * {@code SENDER_ID_MISMATCH} is a token belonging to a different Firebase
	 * project, which this project will never be able to reach however long it
	 * waits.
	 *
	 * <p>
	 * <strong>{@code INVALID_ARGUMENT} is deliberately absent</strong>, and it is
	 * the one that looks like it belongs. FCM returns it for a malformed token,
	 * which is permanent - and also for a payload it will not take, which is our
	 * bug. Listing it here would unregister a working phone every time somebody
	 * shipped a bad notification. Left transient, it costs {@code maxAttempts}
	 * retries and then stops, because {@code ReminderDispatcher} caps them; that is
	 * the cheaper mistake of the two by a wide margin.
	 */
	private static final List<MessagingErrorCode> DEVICE_IS_GONE = List.of(MessagingErrorCode.UNREGISTERED,
			MessagingErrorCode.SENDER_ID_MISMATCH);

	private final FirebaseApp app;

	FirebasePushNotificationProvider(FirebaseProperties properties) {
		boolean configured = properties.credentials().isPresent();

		FirebaseOptions options = FirebaseOptions.builder()
				.setCredentials(properties.credentials().orElseGet(FirebaseProperties::applicationDefault)).build();

		this.app = FirebaseApp.getApps().stream().filter(existing -> APP_NAME.equals(existing.getName())).findFirst()
				.orElseGet(() -> FirebaseApp.initializeApp(options, APP_NAME));

		// Which of the two credential paths took effect. "It works on my machine"
		// and "it works in the container" are different sentences here, and the
		// log is the only place that tells them apart.
		log.info("Push provider is Firebase Cloud Messaging, authenticating with {}",
				configured ? "the configured service account" : "application default credentials");
	}

	@Override
	public void send(String pushToken, PushNotification notification) {
		Message message = Message.builder().setToken(pushToken)
				.setNotification(
						Notification.builder().setTitle(notification.title()).setBody(notification.body()).build())
				.putAllData(notification.data()).build();

		try {
			FirebaseMessaging.getInstance(app).send(message);
		} catch (FirebaseMessagingException refusal) {
			throw meaningOf(refusal.getMessagingErrorCode());
		}
	}

	/**
	 * What a refusal means, as a function of the code and nothing else.
	 *
	 * <p>
	 * <strong>It takes the code rather than the exception so that it can be
	 * tested</strong>, and that is not a compromise made for testing's sake: every
	 * constructor and factory on {@code FirebaseMessagingException} is
	 * package-private, so an exception cannot be built outside Google's own
	 * package. A decision this expensive to get wrong must be checkable, and the
	 * only way to check it was to stop expressing it in terms of a type nobody else
	 * can make.
	 *
	 * <p>
	 * The exception's message is deliberately never read, logged or carried: FCM
	 * echoes the token it was given into some of its errors, and section 27 lists a
	 * full push token among the things that must never reach a log. The code is the
	 * whole of what this project needs.
	 *
	 * <p>
	 * A null code means Firebase failed without classifying it, which is an outage
	 * by another name - transient, and bounded by the dispatcher's attempt cap.
	 */
	static RuntimeException meaningOf(MessagingErrorCode code) {
		if (code != null && DEVICE_IS_GONE.contains(code)) {
			return new PushTokenRejectedException("Firebase reports the token " + code);
		}

		return new ApiException(ErrorCode.PUSH_PROVIDER_UNAVAILABLE);
	}

	/**
	 * Spring closes this on shutdown. Deleting the named app releases its threads
	 * and, more usefully, leaves the static registry as it was found - without
	 * which a second application context in one JVM meets an app that already
	 * exists and cannot say why.
	 */
	@Override
	public void close() {
		app.delete();
	}
}