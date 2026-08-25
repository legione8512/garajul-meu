package ro.garajulmeu.push;

/**
 * The platform has said this token will never work again.
 *
 * <p>
 * Not an {@link ro.garajulmeu.exception.ApiException}, and the distinction is
 * the whole reason this class exists. {@code ErrorCode} declares its own
 * contract - "every failed API response carries exactly one of these", and the
 * frontend maps each to Romanian or English. This never reaches a client and
 * nobody translates it, so adding a code there would have made that sentence
 * false for the sake of reusing an enum.
 *
 * <p>
 * <strong>Permanent, not merely failed.</strong> FCM answers
 * {@code UNREGISTERED} when the application has been uninstalled, which is a
 * fact about the device rather than about the network. Every other refusal -
 * throttling, an outage, a malformed payload - is an {@code ApiException} and
 * is retried. This one is not: retrying it is not patience, it is spending
 * three attempts a minute on a handset that no longer exists.
 *
 * <p>
 * What it means is decided by {@code ReminderDispatcher} and not here, which is
 * the bargain {@link PushNotificationProvider} already strikes: the provider
 * reports what happened, the caller decides what follows.
 */
public class PushTokenRejectedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PushTokenRejectedException(String message) {
		super(message);
	}
}