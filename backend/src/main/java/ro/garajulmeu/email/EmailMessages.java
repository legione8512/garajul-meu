package ro.garajulmeu.email;

import org.springframework.stereotype.Component;

import ro.garajulmeu.auth.AuthProperties;
import ro.garajulmeu.user.Language;

/**
 * The wording of every message this application sends, in both languages
 * section 6 requires.
 *
 * <p><strong>Plain text, and that is a security decision rather than a stylistic
 * one.</strong> The email-change message interpolates an address chosen by
 * whoever requested the change - which, in the case the message exists to catch,
 * is an attacker. In HTML that value is an injection point inside somebody
 * else's mail client. In plain text it is text. The rest follows: no images, no
 * tracking pixel, no external stylesheet, nothing that renders differently
 * across clients, and nothing that can break.
 *
 * <p>The validity is read from configuration rather than written into the
 * sentence. A template that says "15 minutes" beside a setting that says thirty
 * is a small lie told to every user, and nothing would ever catch it.
 */
@Component
public class EmailMessages {

	/** Subject and body, ready to hand to a provider. */
	public record Message(String subject, String body) {
	}

	private final long validityMinutes;

	EmailMessages(AuthProperties properties) {
		// A sub-minute validity would round to zero and produce "valid for 0
		// minutes", which reads as broken rather than as strict. No sane
		// configuration reaches this, and the floor costs one call.
		this.validityMinutes = Math.max(1, properties.verificationCodeValidity().toMinutes());
	}

	Message verification(String code, Language language) {
		return switch (language) {
			case RO -> new Message("Codul tău de verificare — Garajul Meu", """
					Bună,

					Codul tău de verificare pentru Garajul Meu este:

					    %s

					Codul este valabil %d minute și poate fi folosit o singură dată.

					Dacă nu tu ai cerut acest cod, poți ignora acest mesaj — contul tău
					rămâne neatins.

					— Garajul Meu
					""".formatted(code, validityMinutes));

			case EN -> new Message("Your verification code — Garajul Meu", """
					Hello,

					Your Garajul Meu verification code is:

					    %s

					The code is valid for %d minutes and can be used once.

					If you did not ask for this code, you can ignore this message — your
					account is untouched.

					— Garajul Meu
					""".formatted(code, validityMinutes));
		};
	}

	Message passwordReset(String code, Language language) {
		return switch (language) {
			case RO -> new Message("Resetarea parolei — Garajul Meu", """
					Bună,

					Ai cerut resetarea parolei pentru contul tău Garajul Meu. Codul este:

					    %s

					Codul este valabil %d minute și poate fi folosit o singură dată.

					Dacă nu tu ai cerut resetarea, ignoră acest mesaj. Parola ta rămâne
					neschimbată și nimeni nu poate intra în cont fără acest cod.

					— Garajul Meu
					""".formatted(code, validityMinutes));

			case EN -> new Message("Password reset — Garajul Meu", """
					Hello,

					You asked to reset the password on your Garajul Meu account. The code
					is:

					    %s

					The code is valid for %d minutes and can be used once.

					If you did not ask for this, ignore this message. Your password stays
					as it is, and nobody can reach your account without this code.

					— Garajul Meu
					""".formatted(code, validityMinutes));
		};
	}

	/**
	 * Goes to the address currently on the account, never to the requested one -
	 * and names the requested one, because the person who most needs this message
	 * is the one who did not ask for it. A template that only said "confirm the
	 * change" would tell them the least.
	 */
	Message emailChange(String newEmail, String code, Language language) {
		return switch (language) {
			case RO -> new Message("Confirmă schimbarea adresei de email — Garajul Meu", """
					Bună,

					S-a cerut mutarea contului tău Garajul Meu la adresa:

					    %s

					Codul de confirmare este:

					    %s

					Codul este valabil %d minute.

					Dacă nu tu ai cerut această schimbare, NU trimite codul nimănui.
					Contul rămâne pe adresa aceasta atât timp cât codul nu este folosit —
					dar cineva îți cunoaște parola, așa că schimb-o acum.

					— Garajul Meu
					""".formatted(newEmail, code, validityMinutes));

			case EN -> new Message("Confirm your new email address — Garajul Meu", """
					Hello,

					Someone asked to move your Garajul Meu account to:

					    %s

					The confirmation code is:

					    %s

					The code is valid for %d minutes.

					If you did not ask for this change, do NOT give the code to anyone.
					The account stays on this address for as long as the code is unused —
					but somebody knows your password, so change it now.

					— Garajul Meu
					""".formatted(newEmail, code, validityMinutes));
		};
	}
}