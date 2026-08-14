package ro.garajulmeu.email;

import ro.garajulmeu.user.Language;

/**
 * Transactional email, behind an interface so business logic never touches a
 * provider SDK. Specification section 32.
 *
 * <p>Every method takes the recipient's language: section 6 requires templates
 * in Romanian and English, chosen from the account's preference.
 */
public interface EmailProvider {

	void sendVerificationCode(String recipient, String code, Language language);

	/**
	 * Separate from {@link #sendVerificationCode} rather than one method with a
	 * purpose argument: the two templates say different things, and a reader who
	 * receives the wrong wording learns something false about their account.
	 */
	void sendPasswordResetCode(String recipient, String code, Language language);

	/**
	 * Sent to the address currently on the account, never to the requested one.
	 *
	 * <p>Four arguments rather than three, because here the recipient and the
	 * address being asked about are two different things - and that difference is
	 * the whole value of the message. Someone who did not request this change sees
	 * exactly which address another party is trying to move their account to, and
	 * knows to refuse. A template that only said "confirm the change" would tell
	 * the one person who most needs the detail the least.
	 *
	 * @param recipient the account's current address, which receives the code
	 * @param newEmail  the requested address, named in the body so an unexpected
	 *                  change is recognisable
	 */
	void sendEmailChangeCode(String recipient, String newEmail, String code, Language language);
}