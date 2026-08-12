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
 * <p>{@code fieldErrors} is empty for everything except validation failures,
 * where each entry names the rejected field and the constraint it broke, for
 * example {@code NotBlank} or {@code Size}. The frontend turns that pair into a
 * translated message next to the right input.
 */
public record ApiErrorResponse(
		String code,
		int status,
		String path,
		Instant timestamp,
		List<FieldError> fieldErrors) {

	public record FieldError(String field, String constraint) {
	}

	public static ApiErrorResponse of(ErrorCode errorCode, String path) {
		return of(errorCode, path, List.of());
	}

	public static ApiErrorResponse of(ErrorCode errorCode, String path, List<FieldError> fieldErrors) {
		return new ApiErrorResponse(
				errorCode.name(),
				errorCode.status().value(),
				path,
				Instant.now(),
				fieldErrors);
	}
}