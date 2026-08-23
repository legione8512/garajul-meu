package ro.garajulmeu.storage;

import java.net.URI;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Cloudflare R2 through its S3-compatible API. Specification section 32's seam,
 * named there as {@code FileStorageProvider → CloudflareR2FileStorageProvider}.
 *
 * <p>Object keys arrive already UUID-derived, per section 22, so nothing here
 * needs to sanitise one and the log lines below can quote them. There is no
 * path-traversal guard as there is in the local provider: an S3 key is a flat
 * string in a bucket, not a filesystem path, and {@code ../} in one names an
 * object called {@code ../} rather than escaping anywhere.
 *
 * <p><strong>Region {@code auto}.</strong> R2 has no regions in the AWS sense
 * and rejects a real one; the bucket's actual jurisdiction is chosen in
 * Cloudflare's console, where section 23's EU preference is set.
 *
 * <p>If a first deploy fails on uploads with a checksum or "not implemented"
 * error, the cause is the AWS SDK sending the newer {@code x-amz-checksum-*}
 * headers. It is fixed with the environment variable
 * {@code AWS_REQUEST_CHECKSUM_CALCULATION=when_required} and needs no rebuild -
 * which is why it is written here rather than pinned in code for a problem
 * Cloudflare may already have solved.
 */
@Component
@ConditionalOnProperty(name = "garajul-meu.storage.provider", havingValue = "r2")
public class CloudflareR2FileStorageProvider implements FileStorageProvider {

	private static final Logger log = LoggerFactory.getLogger(CloudflareR2FileStorageProvider.class);

	private final S3Client client;

	private final String bucket;

	@Autowired
	CloudflareR2FileStorageProvider(StorageProperties properties) {
		this(properties, clientFor(properties.r2()));
	}

	/** Visible for the test, which supplies a stand-in rather than a network. */
	CloudflareR2FileStorageProvider(StorageProperties properties, S3Client client) {
		this.bucket = required(properties.r2().bucket(), "garajul-meu.storage.r2.bucket");
		this.client = client;

		log.info("Storage provider is R2, bucket {}", bucket);
	}

	private static S3Client clientFor(StorageProperties.R2 r2) {
		return S3Client.builder()
				.endpointOverride(URI.create(endpointFor(r2)))
				.region(Region.of("auto"))
				.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
						required(r2.accessKeyId(), "garajul-meu.storage.r2.access-key-id"),
						required(r2.secretAccessKey(), "garajul-meu.storage.r2.secret-access-key"))))
				.build();
	}

	private static String endpointFor(StorageProperties.R2 r2) {
		if (r2.endpoint() != null && !r2.endpoint().isBlank()) {
			return r2.endpoint();
		}
		return "https://%s.r2.cloudflarestorage.com"
				.formatted(required(r2.accountId(), "garajul-meu.storage.r2.account-id"));
	}

	/**
	 * At startup rather than at the first upload. The shipped configuration
	 * writes {@code ${R2_BUCKET:}} and friends, so an unset variable arrives as
	 * an empty string - which would otherwise become a request R2 refuses, weeks
	 * later, the first time somebody photographs their car.
	 */
	private static String required(String value, String property) {
		if (value == null || value.isBlank()) {
			throw new IllegalStateException(
					property + " must be set when the storage provider is \"r2\"");
		}
		return value;
	}

	@Override
	public void put(String objectKey, byte[] bytes, String contentType) {
		try {
			client.putObject(PutObjectRequest.builder()
							.bucket(bucket)
							.key(objectKey)
							.contentType(contentType)
							.build(),
					RequestBody.fromBytes(bytes));
		} catch (S3Exception exception) {
			throw unavailable("write", objectKey, exception);
		}
	}

	@Override
	public Optional<byte[]> get(String objectKey) {
		try {
			return Optional.of(client.getObjectAsBytes(GetObjectRequest.builder()
					.bucket(bucket)
					.key(objectKey)
					.build()).asByteArray());
		}
		catch (NoSuchKeyException absent) {
			// A fact, not a failure. An object removed outside the application
			// leaves a row pointing at nothing, and the screen that reads it
			// should show a vehicle without a photograph rather than an error.
			return Optional.empty();
		}
		catch (S3Exception exception) {
			throw unavailable("read", objectKey, exception);
		}
	}

	/**
	 * S3 deletes are already idempotent - removing an absent key succeeds - but
	 * NoSuchKeyException is caught anyway, because this method is called from
	 * inside the account-deletion transaction and must never be the reason
	 * somebody's right to be forgotten fails.
	 */
	@Override
	public void delete(String objectKey) {
		try {
			client.deleteObject(DeleteObjectRequest.builder()
					.bucket(bucket)
					.key(objectKey)
					.build());
		}
		catch (NoSuchKeyException alreadyGone) {
			log.info("Nothing to delete at {}; treating as done", objectKey);
		}
		catch (S3Exception exception) {
			throw unavailable("delete", objectKey, exception);
		}
	}

	private ApiException unavailable(String operation, String objectKey, S3Exception cause) {
		log.error("R2 could not {} {}", operation, objectKey, cause);
		return new ApiException(ErrorCode.STORAGE_PROVIDER_UNAVAILABLE, cause);
	}
}