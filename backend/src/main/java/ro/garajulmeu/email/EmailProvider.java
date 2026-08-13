package ro.garajulmeu.email;

import ro.garajulmeu.user.Language;

/**
 * Transactional email, behind an interface so business logic never touches a
 * provider SDK. Specification section 32.
 *
 * <p>Every method takes the recipient's language: section 22 requires templates
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
}