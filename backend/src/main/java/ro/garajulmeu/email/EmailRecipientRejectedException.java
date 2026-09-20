package ro.garajulmeu.email;

/**
 * The email provider refused the recipient itself - it will not send to that
 * address at all - so nothing was delivered and nothing ever will be.
 * GlobalExceptionHandler answers it with EMAIL_UNDELIVERABLE.
 *
 * <p><strong>Deliberately not an {@code ApiException}.</strong> Confirming an
 * email change runs with {@code noRollbackFor = ApiException.class}, so that a
 * wrong code leaves its failed attempt recorded, and it is also the one flow
 * that sends to an address the person has just typed: the new one. Were this an
 * ApiException, a refused new address would be committed to the account -
 * unverified, receiving nothing - while the caller was told the change had
 * failed. As a plain runtime exception it rolls back every flow it can occur
 * in, exactly as the provider failure it used to arrive as did.
 *
 * <p>The message names what was refused, never whom: the address is personal
 * data and a log line has no use for it.
 */
public class EmailRecipientRejectedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public EmailRecipientRejectedException(String message, Throwable cause) {
		super(message, cause);
	}
}
