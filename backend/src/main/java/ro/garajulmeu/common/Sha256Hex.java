package ro.garajulmeu.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * SHA-256 as lowercase hex.
 *
 * <p>Used as a <strong>blind index</strong>: a deterministic digest stored
 * beside a value that is itself encrypted, so a row can still be found and a
 * unique constraint can still bite. Encryption alone forbids both - an
 * authenticated cipher produces different bytes every time by design, which is
 * exactly what makes it safe and exactly what makes it unsearchable.
 *
 * <p>{@code RefreshTokenService} has its own copy of this, written before there
 * was a second caller, and it is deliberately left alone here: moving it would
 * change a file with four test call sites in the middle of a security change.
 * Worth adopting the next time that file is opened for another reason.
 */
public final class Sha256Hex {

	private Sha256Hex() {
	}

	public static String of(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException unreachable) {
			throw new IllegalStateException("Every JVM is required to provide SHA-256", unreachable);
		}
	}
}