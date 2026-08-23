package ro.garajulmeu.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A stand-in S3 client rather than a container, and the reasoning is worth
 * keeping.
 *
 * <p>A MinIO container would prove the AWS SDK round-trips - but the SDK is not
 * this project's code. What is, and what can be wrong, is the key, the bucket,
 * the content type and the mapping from an S3 exception to this application's
 * error codes. All of that is asserted here without a fifth container on every
 * build.
 *
 * <p><strong>What no test can prove is the part most likely to be wrong:</strong>
 * that the endpoint, the {@code auto} region and the R2 credentials are right.
 * MinIO could not prove that either - it is not Cloudflare. That is settled by
 * the first deploy against a real bucket, and the startup log line naming the
 * bucket is the first thing to read when it is.
 */
class CloudflareR2FileStorageProviderTest {

	private static final String KEY = "vehicles/2f1a8c2e-0b17-4b2e-9a3a-6d5f0c3e91aa.jpg";

	private final S3Client s3 = mock(S3Client.class);

	private static StorageProperties properties(String bucket) {
		return new StorageProperties("r2", "./storage", 0, 0, 0,
				new StorageProperties.R2("acc", "key", "secret", bucket, null));
	}

	private CloudflareR2FileStorageProvider provider() {
		return new CloudflareR2FileStorageProvider(properties("garajul-meu-images"), s3);
	}

	@Test
	void writesTheBytesUnderTheKeyWithTheContentTypeItWasGiven() throws IOException {
		ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
		ArgumentCaptor<RequestBody> body = ArgumentCaptor.forClass(RequestBody.class);

		provider().put(KEY, "a photograph".getBytes(StandardCharsets.UTF_8), "image/jpeg");

		verify(s3).putObject(request.capture(), body.capture());

		assertThat(request.getValue().bucket()).isEqualTo("garajul-meu-images");
		assertThat(request.getValue().key()).isEqualTo(KEY);
		assertThat(request.getValue().contentType()).isEqualTo("image/jpeg");
		assertThat(body.getValue().contentStreamProvider().newStream().readAllBytes())
				.asString(StandardCharsets.UTF_8)
				.isEqualTo("a photograph");
	}

	@Test
	void readsBackWhatIsThere() {
		when(s3.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(ResponseBytes.fromByteArray(
				GetObjectResponse.builder().build(), "a photograph".getBytes(StandardCharsets.UTF_8)));

		// orElseThrow rather than AssertJ's .get(): the latter answers an
		// AbstractObjectAssert, which has forgotten these are bytes and offers no
		// asString(Charset). Unwrapping first keeps the byte[] assertions.
		assertThat(provider().get(KEY).orElseThrow())
				.asString(StandardCharsets.UTF_8)
				.isEqualTo("a photograph");
	}

	/**
	 * Empty rather than an exception, because the interface says so: a row
	 * pointing at an object somebody removed in Cloudflare's console should show
	 * a vehicle without a photograph, not an error screen.
	 */
	@Test
	void anAbsentObjectIsAFactAndNotAFailure() {
		when(s3.getObjectAsBytes(any(GetObjectRequest.class)))
				.thenThrow(NoSuchKeyException.builder().message("no such key").build());

		assertThat(provider().get(KEY)).isEqualTo(Optional.empty());
	}

	/**
	 * The other direction, and the one that makes the test above mean something.
	 * If every S3 exception became an empty Optional, a bucket that had stopped
	 * answering would look exactly like a garage where nobody had uploaded
	 * anything.
	 */
	@Test
	void aBucketThatCannotBeReachedIsAFailureAndSaysSo() {
		when(s3.getObjectAsBytes(any(GetObjectRequest.class)))
				.thenThrow(S3Exception.builder().message("service unavailable").build());

		assertThatThrownBy(() -> provider().get(KEY))
				.isInstanceOf(ApiException.class)
				.extracting(exception -> ((ApiException) exception).errorCode())
				.isEqualTo(ErrorCode.STORAGE_PROVIDER_UNAVAILABLE);
	}

	/**
	 * Deleting is called from inside the account-deletion transaction, where an
	 * exception would roll the deletion back and make somebody's right to be
	 * forgotten depend on a bucket answering.
	 */
	@Test
	void deletingSomethingAlreadyGoneSucceeds() {
		ArgumentCaptor<DeleteObjectRequest> request = ArgumentCaptor.forClass(DeleteObjectRequest.class);

		when(s3.deleteObject(any(DeleteObjectRequest.class)))
				.thenThrow(NoSuchKeyException.builder().message("no such key").build());

		provider().delete(KEY);

		verify(s3).deleteObject(request.capture());
		assertThat(request.getValue().key()).isEqualTo(KEY);
	}

	@Test
	void refusesToStartWithoutABucket() {
		assertThatThrownBy(() -> new CloudflareR2FileStorageProvider(properties(""), s3))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("bucket");
	}

	/**
	 * The credential path, which the constructor above bypasses because a test
	 * hands in its own client. Building an S3Client opens no connection, so this
	 * reaches the validation without touching a network.
	 */
	@Test
	void refusesToStartWithoutCredentials() {
		StorageProperties keyless = new StorageProperties("r2", "./storage", 0, 0, 0,
				new StorageProperties.R2("acc", "", "secret", "bucket", null));

		assertThatThrownBy(() -> new CloudflareR2FileStorageProvider(keyless))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("access-key-id");
	}
}