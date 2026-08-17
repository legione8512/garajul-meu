package ro.garajulmeu.push;

/**
 * Native push, behind an interface so business logic never touches Firebase.
 * Specification sections 32 and 36, the same bargain as {@code EmailProvider}
 * and {@code OcrProvider}.
 *
 * <p>Takes the token rather than the device, so an implementation needs no
 * repository and no entity - and so that whatever protects the token at rest can
 * change without this interface noticing.
 */
public interface PushNotificationProvider {

	/**
	 * @throws ro.garajulmeu.exception.ApiException when the provider refuses. The
	 *         caller in 11.4 records the code against the delivery and retries;
	 *         nothing here decides what a failure means.
	 */
	void send(String pushToken, PushNotification notification);
}