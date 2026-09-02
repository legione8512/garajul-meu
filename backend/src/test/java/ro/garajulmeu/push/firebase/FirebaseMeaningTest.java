package ro.garajulmeu.push.firebase;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.google.firebase.messaging.MessagingErrorCode;

import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;
import ro.garajulmeu.push.PushTokenRejectedException;

/**
 * The error mapping, which is the whole of the Firebase adapter worth checking.
 *
 * <p>
 * Sending is four lines and needs a network. Deciding what a refusal
 * <em>means</em> is the part with a cost in both directions, and the interface
 * says so: a dead handset reported as transient buys retries for ever, and an
 * outage reported as a dead handset unregisters a phone that was working ten
 * seconds ago.
 */
class FirebaseMeaningTest {

	private static final List<MessagingErrorCode> GONE = List.of(MessagingErrorCode.UNREGISTERED,
			MessagingErrorCode.SENDER_ID_MISMATCH);

	@ParameterizedTest
	@EnumSource(MessagingErrorCode.class)
	void everyCodeMeansEitherADeadDeviceOrAMomentToRetry(MessagingErrorCode code) {
		RuntimeException meaning = FirebasePushNotificationProvider.meaningOf(code);

		if (GONE.contains(code)) {
			assertThat(meaning).isInstanceOf(PushTokenRejectedException.class);
		} else {
			assertThat(meaning).isInstanceOf(ApiException.class)
					.extracting(thrown -> ((ApiException) thrown).errorCode())
					.isEqualTo(ErrorCode.PUSH_PROVIDER_UNAVAILABLE);
		}
	}

	/**
	 * The tempting mistake, asserted on its own so it cannot be undone quietly.
	 *
	 * <p>
	 * FCM returns {@code INVALID_ARGUMENT} for a malformed token, which is
	 * permanent - and for a payload it will not take, which is our bug. Treating it
	 * as a dead device would unregister a working phone every time somebody shipped
	 * a bad notification. Left transient it costs the dispatcher's capped retries
	 * and stops, which is the cheaper of the two mistakes by a wide margin.
	 */
	@Test
	void doesNotUnregisterADeviceBecauseFirebaseDislikedTheRequest() {
		assertThat(FirebasePushNotificationProvider.meaningOf(MessagingErrorCode.INVALID_ARGUMENT))
				.isNotInstanceOf(PushTokenRejectedException.class);
	}

	/** Firebase failing without classifying it is an outage by another name. */
	@Test
	void treatsAnUnclassifiedFailureAsSomethingToRetry() {
		assertThat(FirebasePushNotificationProvider.meaningOf(null)).isInstanceOf(ApiException.class);
	}

	/**
	 * The guard that earns its keep on an SDK upgrade rather than today.
	 *
	 * <p>
	 * A code Firebase adds later would default to transient and nothing would say
	 * so - it would simply be retried a few times and dropped, for ever, with
	 * nobody having decided that was right. Pinning the count turns the next
	 * upgrade into one failing test and one deliberate decision. Same shape as
	 * `errorKey.test.ts`, and written for the same reason: a list that only grows
	 * when somebody remembers is a list that stops growing.
	 */
	@Test
	void namesEveryCodeTheSdkCurrentlyDefines() {
		assertThat(Arrays.stream(MessagingErrorCode.values()).map(Enum::name)).containsExactlyInAnyOrder(
				"INVALID_ARGUMENT", "INTERNAL", "QUOTA_EXCEEDED", "SENDER_ID_MISMATCH", "THIRD_PARTY_AUTH_ERROR",
				"UNAVAILABLE", "UNREGISTERED");
	}
}