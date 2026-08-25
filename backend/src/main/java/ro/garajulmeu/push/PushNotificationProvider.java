package ro.garajulmeu.push;

/**
 * Native push, behind an interface so business logic never touches Firebase.
 * Specification sections 32 and 36, the same bargain as {@code EmailProvider}
 * and {@code OcrProvider}.
 *
 * <p>
 * Takes the token rather than the device, so an implementation needs no
 * repository and no entity - and so that whatever protects the token at rest
 * can change without this interface noticing.
 */
public interface PushNotificationProvider {

	/**
	 * @throws ro.garajulmeu.exception.ApiException when the provider refuses and
	 *                                              the refusal might not last -
	 *                                              throttling, an outage, a payload
	 *                                              the platform would not take. The
	 *                                              caller records the code against
	 *                                              the delivery and retries;
	 *                                              nothing here decides what a
	 *                                              failure means.
	 * @throws PushTokenRejectedException           when the platform reports the
	 *                                              token permanently invalid, which
	 *                                              is a different kind of answer
	 *                                              entirely: it describes the
	 *                                              device rather than the moment.
	 *                                              An implementation must not
	 *                                              translate one into the other -
	 *                                              reporting a dead handset as a
	 *                                              transient failure buys it three
	 *                                              retries a minute for ever, and
	 *                                              reporting an outage as a dead
	 *                                              handset unregisters a phone that
	 *                                              was working ten seconds ago.
	 */
	void send(String pushToken, PushNotification notification);
}