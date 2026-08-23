package ro.garajulmeu.push;

import java.util.Base64;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** No Spring: a key and some strings. */
class PushTokenCipherTest {

	private static final String SECRET = "test-only-push-token-key-never-used-outside-tests";

	private static final String TOKEN =
			"fZx1qK9mTUe0aB2cD3eF4g:APA91bHqL8vN2mP5rS7tU9wX1yZ3aC5eG7iK9mO1qS3uW5y";

	private final PushTokenCipher cipher = new PushTokenCipher(new PushProperties("logging", SECRET));

	@Test
	void whatIsEncryptedComesBackExactly() {
		assertThat(cipher.decrypt(cipher.encrypt(TOKEN))).isEqualTo(TOKEN);
	}

	/**
	 * <strong>The property that forced the blind index.</strong> A fresh IV per
	 * encryption is what makes the cipher safe, and it is also why
	 * {@code findByPushToken} could not survive: the same token written twice
	 * produces two different columns, so no equality search can ever match. If
	 * this test ever passed by returning identical strings, the encryption would
	 * be broken and the hash column pointless at the same time.
	 */
	@Test
	void theSameTokenEncryptsDifferentlyEveryTime() {
		assertThat(cipher.encrypt(TOKEN)).isNotEqualTo(cipher.encrypt(TOKEN));
	}

	/** Section 27 keeps tokens out of logs; this keeps them out of the column too. */
	@Test
	void theStoredFormRevealsNothingOfTheToken() {
		String stored = cipher.encrypt(TOKEN);

		assertThat(stored).doesNotContain(TOKEN);
		assertThat(new String(Base64.getDecoder().decode(stored)))
				.as("the decoded bytes, in case base64 was the only thing hiding it")
				.doesNotContain(TOKEN);
	}

	/**
	 * GCM authenticates as well as encrypts, so a row written under a different
	 * key does not decrypt to plausible nonsense - it refuses. That is the
	 * difference that matters if the key is ever rotated without a plan: the
	 * application stops, rather than sending notifications to addresses it
	 * invented.
	 */
	@Test
	void aTokenSealedUnderAnotherKeyIsRefusedRatherThanMisread() {
		PushTokenCipher other = new PushTokenCipher(
				new PushProperties("logging", "a-different-key-of-at-least-thirty-two-chars"));

		String sealed = other.encrypt(TOKEN);

		assertThatThrownBy(() -> cipher.decrypt(sealed))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("key may have changed");
	}

	/**
	 * At startup, not at the first registration - and the message names the
	 * property, because the alternative is a stack trace about key lengths that
	 * says nothing about which setting is missing.
	 */
	@Test
	void refusesAKeyThatIsMissingOrTooShortToBeWorthDeriving() {
		assertThatThrownBy(() -> new PushTokenCipher(new PushProperties("logging", null)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("garajul-meu.push.token-key");

		assertThatThrownBy(() -> new PushTokenCipher(new PushProperties("logging", "too-short")))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("32 characters");
	}
}