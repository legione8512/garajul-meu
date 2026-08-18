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
 *                       {@code r2} is Cloudflare R2 and arrives in 12.4
 * @param localDirectory only read by the local provider. Gitignored: it holds
 *                       real photographs of real cars
 * @param maxUploadBytes five megabytes, which is a photograph from a phone
 *                       rather than a photograph from a camera
 */
@ConfigurationProperties(prefix = "garajul-meu.storage")
public record StorageProperties(
		@DefaultValue("local") String provider,
		@DefaultValue("./storage") String localDirectory,
		long maxUploadBytes,
		int minSide,
		int maxPixels) {

	public StorageProperties {
		maxUploadBytes = maxUploadBytes > 0 ? maxUploadBytes : 5L * 1024 * 1024;
		minSide = minSide > 0 ? minSide : 200;
		maxPixels = maxPixels > 0 ? maxPixels : 40_000_000;
	}
}