package ro.garajulmeu.exception;

import java.time.Instant;
import java.util.List;

/**
 * The single JSON shape returned by every failed request.
 *
 * <p>There is deliberately no human-readable message field. Adding one would
 * invite the frontend to display server English, defeating the bilingual
 * requirement in specification section 6.
 *
 * <p>{@code requestId} is the correlation identifier of the failing request. A
 * user can quote it from an error screen and we can find that exact request in
 * the logs without knowing their account or the time of day.
 *
 * <p>{@code fieldErrors} is empty for everything except validation failures,
 * where each entry names the rejected field and the constraint it broke, for
 * example {@code NotBlank} or {@code Size}. The frontend turns that pair into a
 * translated message next to the right input.
 */
public record ApiErrorResponse(
		String code,
		int status,
		String path,
		String requestId,
		Instant timestamp,
		List<FieldError> fieldErrors) {

	public record FieldError(String field, String constraint) {
	}

	public static ApiErrorResponse of(ErrorCode errorCode, String path, String requestId) {
		return of(errorCode, path, requestId, List.of());
	}

	public static ApiErrorResponse of(ErrorCode errorCode, String path, String requestId,
			List<FieldError> fieldErrors) {
		return new ApiErrorResponse(
				errorCode.name(),
				errorCode.status().value(),
				path,
				requestId,
				Instant.now(),
				fieldErrors);
	}
}