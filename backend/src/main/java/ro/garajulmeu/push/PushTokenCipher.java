package ro.garajulmeu.push;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

/**
 * Encrypts push tokens at rest, per specification section 10.7.
 *
 * <p>The requirement is unusual and worth restating, because it is the opposite
 * of how refresh tokens are handled two packages away: <em>"user_devices must
 * retain a retrievable token value (for example encrypted at
 * application/infrastructure level), not a one-way hash only"</em>. FCM needs
 * the value in order to send anything, so a digest would make the column
 * useless. A refresh token is only ever compared, so a digest is all it gets.
 *
 * <p><strong>AES-256-GCM</strong>, which authenticates as well as encrypts: a
 * row edited in the database does not decrypt to something plausible, it fails
 * to decrypt at all. A fresh twelve-byte IV per encryption is generated and
 * stored in front of the ciphertext, which is what makes two encryptions of the
 * same token differ - and therefore why {@link UserDevice} carries a separate
 * hash for anything that needs to find a row.
 *
 * <p>The key is derived from a configured secret by SHA-256, the same shape
 * {@code JwtProperties.secret} already uses for HMAC: it accepts a passphrase
 * rather than demanding base64 of exactly thirty-two bytes, which is one fewer
 * way to be wrong at three in the morning. A minimum length is enforced because
 * the derivation cannot add entropy that was never there.
 */
@Component
public class PushTokenCipher {

	private static final String TRANSFORMATION = "AES/GCM/NoPadding";

	private static final int IV_BYTES = 12;

	private static final int TAG_BITS = 128;

	private static final int MINIMUM_SECRET_LENGTH = 32;

	private final SecretKey key;

	private final SecureRandom random = new SecureRandom();

	PushTokenCipher(PushProperties properties) {
		this.key = keyFrom(properties.tokenKey());
	}

	public String encrypt(String plaintext) {
		byte[] iv = new byte[IV_BYTES];
		random.nextBytes(iv);

		try {
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));

			byte[] sealed = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
			byte[] stored = new byte[iv.length + sealed.length];

			System.arraycopy(iv, 0, stored, 0, iv.length);
			System.arraycopy(sealed, 0, stored, iv.length, sealed.length);

			return Base64.getEncoder().encodeToString(stored);
		}
		catch (GeneralSecurityException failure) {
			// Nothing here depends on the input: a failure means the JVM or the
			// key is wrong, which is a startup-grade problem discovered late.
			// The message deliberately names neither the token nor the key.
			throw new IllegalStateException("Could not encrypt a push token", failure);
		}
	}

	public String decrypt(String stored) {
		byte[] bytes = Base64.getDecoder().decode(stored);

		if (bytes.length <= IV_BYTES) {
			throw new IllegalStateException("A stored push token is too short to contain an IV");
		}

		try {
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, key,
					new GCMParameterSpec(TAG_BITS, bytes, 0, IV_BYTES));

			return new String(cipher.doFinal(bytes, IV_BYTES, bytes.length - IV_BYTES),
					StandardCharsets.UTF_8);
		}
		catch (GeneralSecurityException failure) {
			// GCM refusing the tag means one of two things, and both are worth
			// stopping for: the key has changed, or the row was edited outside
			// the application. Neither should ever produce a usable token.
			throw new IllegalStateException(
					"A stored push token could not be decrypted; the key may have changed", failure);
		}
	}

	private static SecretKey keyFrom(String secret) {
		if (secret == null || secret.length() < MINIMUM_SECRET_LENGTH) {
			throw new IllegalStateException(
					"garajul-meu.push.token-key must be set and at least "
							+ MINIMUM_SECRET_LENGTH + " characters");
		}

		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return new SecretKeySpec(digest.digest(secret.getBytes(StandardCharsets.UTF_8)), "AES");
		}
		catch (NoSuchAlgorithmException unreachable) {
			throw new IllegalStateException("Every JVM is required to provide SHA-256", unreachable);
		}
	}
}