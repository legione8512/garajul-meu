package ro.garajulmeu.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Where vehicle images go and what one may be. Specification section 22.
 *
 * <p>The limits are the same three OCR uses and the numbers are not. An upload
 * here is kept rather than read once and discarded, so the ceiling is lower;
 * the smallest edge is the same, because a picture of a car below two hundred
 * pixels is a favicon whichever feature receives it.
 *
 * @param provider       {@code local} writes to the filesystem for development;
 *                       {@code r2} is Cloudflare R2
 * @param localDirectory only read by the local provider. Gitignored: it holds
 *                       real photographs of real cars
 * @param maxUploadBytes five megabytes, which is a photograph from a phone
 *                       rather than a photograph from a camera
 * @param r2             only read by the R2 provider, which refuses to start
 *                       without it. Never null - see the compact constructor
 */
@ConfigurationProperties(prefix = "garajul-meu.storage")
public record StorageProperties(
		@DefaultValue("local") String provider,
		@DefaultValue("./storage") String localDirectory,
		long maxUploadBytes,
		int minSide,
		int maxPixels,
		@DefaultValue R2 r2) {

	public StorageProperties {
		maxUploadBytes = maxUploadBytes > 0 ? maxUploadBytes : 5L * 1024 * 1024;
		minSide = minSide > 0 ? minSide : 200;
		maxPixels = maxPixels > 0 ? maxPixels : 40_000_000;

		// An empty R2 rather than null, so `properties.r2().bucket()` is always a
		// safe call and the R2 provider's own "must be set" message is what a
		// misconfiguration produces - naming the property - instead of a
		// NullPointerException naming nothing. Spring's binder already supplies
		// one when the block is absent; this covers the callers that construct
		// the record directly, which is every test of the local provider.
		r2 = r2 != null ? r2 : new R2(null, null, null, null, null);
	}

	/**
	 * Cloudflare R2, reached through the S3 API.
	 *
	 * @param accountId       names the endpoint,
	 *                        {@code https://<id>.r2.cloudflarestorage.com}. Not a
	 *                        secret - it identifies an account, it does not open
	 *                        one - but it arrives from the environment anyway,
	 *                        beside the keys it belongs with
	 * @param accessKeyId     from an R2 API token
	 * @param secretAccessKey from the same token, and shown by Cloudflare once
	 * @param bucket          section 22: private, and EU jurisdiction where
	 *                        supported
	 * @param endpoint        overridable only so a test or an S3-compatible
	 *                        stand-in can point somewhere that is not Cloudflare.
	 *                        Left empty it is derived from the account id
	 */
	public record R2(
			String accountId,
			String accessKeyId,
			String secretAccessKey,
			String bucket,
			String endpoint) {
	}
}