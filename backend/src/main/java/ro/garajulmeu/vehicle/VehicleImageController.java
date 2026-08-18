package ro.garajulmeu.vehicle;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;
import ro.garajulmeu.storage.StorageProperties;
import ro.garajulmeu.vehicle.dto.VehicleImageContent;

/**
 * Section 16's two image endpoints, and the third its own prose sanctions:
 * "a vehicle response may expose an authorised short-lived image URL <em>or an
 * application endpoint</em>". This is that endpoint.
 *
 * <p><strong>The upload is the raw body, not multipart.</strong> Tomcat parses
 * {@code multipart/form-data} for POST only - a PUT arrives with no parts at all
 * - so multipart here would have meant either changing the method the contract
 * specifies or turning on casual multipart parsing container-wide. A single
 * binary resource is a better fit for a raw body anyway, and the declared
 * content type is ignored either way: the validator reads the format from the
 * bytes.
 *
 * <p><strong>What that costs, and how it is paid.</strong> Multipart brought a
 * free size limit from {@code spring.servlet.multipart.max-file-size}; a raw
 * body has no ceiling at all, and {@code @RequestBody byte[]} would read a two
 * gigabyte PUT into memory before anything could object. So the body is read
 * bounded, one byte past the limit, and refused there - which also makes the
 * limit ours rather than the container's, and therefore something a client can
 * actually be told about instead of having its connection closed.
 */
@RestController
@RequestMapping("/api/v1/vehicles/{vehicleId}/image")
public class VehicleImageController {

	private final VehicleImageService imageService;
	private final StorageProperties properties;

	VehicleImageController(VehicleImageService imageService, StorageProperties properties) {
		this.imageService = imageService;
		this.properties = properties;
	}

	/**
	 * <p>{@code no-store} rather than a long cache, and that follows from the
	 * address: this path is the vehicle's and never changes, while the photograph
	 * behind it does. A cached response would show the previous picture after a
	 * replacement with nothing to invalidate it. It is also personal data, which
	 * makes a shared cache the wrong place for it twice over.
	 */
	@GetMapping
	public ResponseEntity<byte[]> image(@AuthenticationPrincipal Jwt token,
			@PathVariable UUID vehicleId) {
		VehicleImageContent content = imageService.read(accountOf(token), vehicleId);

		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(content.contentType()))
				.cacheControl(CacheControl.noStore())
				.body(content.bytes());
	}

	@PutMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void replace(@AuthenticationPrincipal Jwt token, @PathVariable UUID vehicleId,
			HttpServletRequest request) throws IOException {
		imageService.replace(accountOf(token), vehicleId, readBounded(request));
	}

	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@AuthenticationPrincipal Jwt token, @PathVariable UUID vehicleId) {
		imageService.remove(accountOf(token), vehicleId);
	}

	/**
	 * Reads at most one byte more than the limit allows, and refuses if it got
	 * that byte.
	 *
	 * <p>Reading the declared Content-Length instead would trust the sender, and a
	 * chunked request declares nothing at all. This asks the stream rather than
	 * the header, which is the same principle the validator applies to the format.
	 */
	private byte[] readBounded(HttpServletRequest request) throws IOException {
		long limit = properties.maxUploadBytes();

		try (InputStream stream = request.getInputStream()) {
			byte[] bytes = stream.readNBytes((int) Math.min(limit + 1, Integer.MAX_VALUE));

			if (bytes.length > limit) {
				throw new ApiException(ErrorCode.IMAGE_TOO_LARGE);
			}
			return bytes;
		}
	}

	private static UUID accountOf(Jwt token) {
		return UUID.fromString(token.getSubject());
	}
}