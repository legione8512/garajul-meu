package ro.garajulmeu.exception;

import java.util.List;
import org.slf4j.MDC;
import ro.garajulmeu.common.RequestIdFilter;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Turns every exception into the one {@link ApiErrorResponse} shape.
 *
 * <p>Stack traces, exception messages and class names must never reach the
 * client. Section 17 leaves nowhere to put them: a failure answers with one
 * stable code that the frontend translates, and nothing else. Only handled,
 * expected failures name a specific code; everything else collapses to
 * INTERNAL_ERROR.
 *
 * <p>Log levels follow specification section 27. Expected 4xx outcomes are
 * business events, logged at INFO. Only genuinely unhandled exceptions are
 * logged at ERROR, which is what Sentry will pick up from Phase 15 onwards.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ApiErrorResponse> handleApiException(
			ApiException exception, HttpServletRequest request) {
		ErrorCode code = exception.errorCode();
		log.info("Business failure {} on {} {}", code, request.getMethod(), request.getRequestURI());
		return respond(code, request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidation(
			MethodArgumentNotValidException exception, HttpServletRequest request) {

		List<ApiErrorResponse.FieldError> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> new ApiErrorResponse.FieldError(error.getField(), error.getCode()))
				.toList();

		log.info("Validation failed on {} {} for {}",
				request.getMethod(), request.getRequestURI(),
				fieldErrors.stream().map(ApiErrorResponse.FieldError::field).toList());

		return ResponseEntity
				.status(ErrorCode.VALIDATION_ERROR.status())
				.body(ApiErrorResponse.of(ErrorCode.VALIDATION_ERROR, request.getRequestURI(),
						MDC.get(RequestIdFilter.MDC_KEY), fieldErrors));
	}

	/**
	 * A path or query value that will not convert - most often an identifier that
	 * is not a UUID. Without this it reaches the catch-all below and answers 500,
	 * logged at ERROR, so from Phase 15 a mistyped URL would raise a Sentry alert
	 * about a fault that is entirely the caller's. It is a client mistake, and it
	 * says so.
	 */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiErrorResponse> handleUnconvertibleParameter(HttpServletRequest request) {
		log.info("Unconvertible parameter on {} {}", request.getMethod(), request.getRequestURI());
		return respond(ErrorCode.VALIDATION_ERROR, request);
	}

	/**
	 * A required part or header that never arrived - a multipart request with no
	 * file in it, for instance. Both types are named because they are unrelated:
	 * MissingServletRequestPartException extends ServletException and is not a
	 * ServletRequestBindingException, so handling only the latter leaves the
	 * multipart case falling through to the catch-all and answering 500 for
	 * something the caller did.
	 */
	@ExceptionHandler({ MissingServletRequestPartException.class, ServletRequestBindingException.class })
	public ResponseEntity<ApiErrorResponse> handleMissingPart(HttpServletRequest request) {
		log.info("Missing part or parameter on {} {}", request.getMethod(), request.getRequestURI());
		return respond(ErrorCode.MALFORMED_REQUEST, request);
	}

	/**
	 * An upload the servlet container refused before anything of ours saw it.
	 * IMAGE_TOO_LARGE rather than a generic failure because it is exactly what
	 * happened and the catalogue already has the word for it; the code is not
	 * OCR-specific, and Phase 12's vehicle images will arrive here too.
	 */
	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ApiErrorResponse> handleOversizedUpload(HttpServletRequest request) {
		log.info("Upload over the container limit on {} {}", request.getMethod(), request.getRequestURI());
		return respond(ErrorCode.IMAGE_TOO_LARGE, request);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiErrorResponse> handleMalformedBody(HttpServletRequest request) {
		log.info("Malformed request body on {} {}", request.getMethod(), request.getRequestURI());
		return respond(ErrorCode.MALFORMED_REQUEST, request);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleUnknownRoute(HttpServletRequest request) {
		log.info("No route for {} {}", request.getMethod(), request.getRequestURI());
		return respond(ErrorCode.RESOURCE_NOT_FOUND, request);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(HttpServletRequest request) {
		log.info("Method not allowed for {} {}", request.getMethod(), request.getRequestURI());
		return respond(ErrorCode.METHOD_NOT_ALLOWED, request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleUnexpected(
			Exception exception, HttpServletRequest request) {
		log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), exception);
		return respond(ErrorCode.INTERNAL_ERROR, request);
	}

	private ResponseEntity<ApiErrorResponse> respond(ErrorCode code, HttpServletRequest request) {
		return ResponseEntity
				.status(code.status())
				.body(ApiErrorResponse.of(code, request.getRequestURI(), MDC.get(RequestIdFilter.MDC_KEY)));
	}
}