package ro.garajulmeu.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;

/**
 * Writes objects to a directory on this machine instead of to a bucket.
 *
 * <p><strong>Development only</strong>, and the warning says so at startup, as
 * the email and push providers do. Selected by an explicit property, so
 * production cannot fall back to it silently - with the property absent there is
 * no FileStorageProvider bean and the application refuses to start.
 *
 * <p>The directory is created on first write rather than at startup. Every
 * Spring test in this project constructs this bean, and a constructor that made
 * directories would leave one in the working tree after every test run for a
 * feature the test never used.
 *
 * <p>The content type is accepted and ignored. A filesystem has nowhere to put
 * it, and it does not need one: section 22 keeps object metadata in PostgreSQL,
 * so {@code vehicles.image_content_type} answers that question for both
 * providers. Writing a sidecar file would create a second copy whose only
 * possible future is to disagree.
 */
@Component
@ConditionalOnProperty(name = "garajul-meu.storage.provider", havingValue = "local")
public class LocalFileStorageProvider implements FileStorageProvider {

	private static final Logger log = LoggerFactory.getLogger(LocalFileStorageProvider.class);

	private final Path root;

	LocalFileStorageProvider(StorageProperties properties) {
		this.root = Path.of(properties.localDirectory()).toAbsolutePath().normalize();

		log.warn("Storage provider is LOCAL: vehicle images are written to {} and exist only on "
				+ "this machine. Production must override this with the R2 provider.", root);
	}

	@Override
	public void put(String objectKey, byte[] bytes, String contentType) {
		Path target = resolve(objectKey);

		try {
			Files.createDirectories(target.getParent());
			Files.write(target, bytes);
		} catch (IOException exception) {
			throw unavailable("write", objectKey, exception);
		}
	}

	@Override
	public Optional<byte[]> get(String objectKey) {
		Path source = resolve(objectKey);

		if (!Files.isRegularFile(source)) {
			return Optional.empty();
		}

		try {
			return Optional.of(Files.readAllBytes(source));
		} catch (IOException exception) {
			throw unavailable("read", objectKey, exception);
		}
	}

	@Override
	public void delete(String objectKey) {
		try {
			Files.deleteIfExists(resolve(objectKey));
		} catch (IOException exception) {
			throw unavailable("delete", objectKey, exception);
		}
	}

	/**
	 * The key joined to the root, and never outside it.
	 *
	 * <p>Every key this application stores is built from UUIDs, so nothing
	 * reachable today can escape. The guard is here because "the caller generates
	 * the key" is a property of today's callers rather than of this class, and a
	 * path traversal discovered later is a far worse thing to find than a check
	 * that never fired.
	 *
	 * <p>It throws IllegalArgumentException rather than an ApiException, so the
	 * handler answers INTERNAL_ERROR: a key that escapes the root is our mistake,
	 * not a store that is unavailable, and 500 is the honest thing to say about it.
	 */
	private Path resolve(String objectKey) {
		Path candidate = root.resolve(objectKey).normalize();

		if (!candidate.startsWith(root)) {
			log.error("Refused an object key that resolves outside the storage root");
			throw new IllegalArgumentException("Object key escapes the storage root");
		}
		return candidate;
	}

	/** The key is safe to log: section 22 keeps everything identifying out of it. */
	private ApiException unavailable(String operation, String objectKey, IOException cause) {
		log.error("Local storage could not {} {}", operation, objectKey, cause);
		return new ApiException(ErrorCode.STORAGE_PROVIDER_UNAVAILABLE, cause);
	}
}