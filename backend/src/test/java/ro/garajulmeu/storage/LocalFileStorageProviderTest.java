package ro.garajulmeu.storage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** No Spring: a directory and some bytes. */
class LocalFileStorageProviderTest {

	@TempDir
	private Path directory;

	private static final byte[] BYTES = { 1, 2, 3, 4, 5 };

	private LocalFileStorageProvider provider() {
		return new LocalFileStorageProvider(
				new StorageProperties("local", directory.toString(), 0, 0, 0));
	}

	/** The shape section 22 requires: UUIDs and nothing else in the path. */
	private static String key() {
		return "vehicles/" + UUID.randomUUID() + "/" + UUID.randomUUID();
	}

	@Test
	void whatIsWrittenComesBackByteForByte() {
		LocalFileStorageProvider provider = provider();
		String objectKey = key();

		provider.put(objectKey, BYTES, "image/jpeg");

		assertThat(provider.get(objectKey)).contains(BYTES);
	}

	/**
	 * Nested keys make their own directories. Worth a test rather than an
	 * assumption: R2 has no directories at all, so a key with slashes in it is
	 * one flat name there and a tree here, and only one of the two needs mkdir.
	 */
	@Test
	void aKeyWithSlashesInItCreatesTheDirectoriesItNeeds() {
		LocalFileStorageProvider provider = provider();
		String objectKey = key();

		provider.put(objectKey, BYTES, "image/png");

		assertThat(Files.isRegularFile(directory.resolve(objectKey))).isTrue();
	}

	/**
	 * A row can point at an object somebody removed from the bucket. That is a
	 * vehicle without a photograph, not an error, and the interface says so by
	 * answering an empty Optional.
	 */
	@Test
	void aKeyThatWasNeverWrittenIsEmptyRatherThanAFailure() {
		assertThat(provider().get(key())).isEmpty();
	}

	@Test
	void deletingRemovesItAndDeletingAgainIsNotAnError() {
		LocalFileStorageProvider provider = provider();
		String objectKey = key();

		provider.put(objectKey, BYTES, "image/jpeg");
		provider.delete(objectKey);

		assertThat(provider.get(objectKey)).isEmpty();

		provider.delete(objectKey);
	}

	/**
	 * Nothing in this application can produce such a key - they are built from
	 * UUIDs - which is exactly why the guard is worth a test: it protects against
	 * a caller that does not exist yet, and an untested guard is a comment.
	 */
	@Test
	void aKeyThatClimbsOutOfTheRootIsRefused() {
		LocalFileStorageProvider provider = provider();

		assertThatThrownBy(() -> provider.put("../escaped", BYTES, "image/jpeg"))
				.isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> provider.get("vehicles/../../escaped"))
				.isInstanceOf(IllegalArgumentException.class);
	}
}