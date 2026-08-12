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
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Turns every exception into the one {@link ApiErrorResponse} shape.
 *
 * <p>Specification section 30: stack traces, exception messages and class names
 * must never reach the client. Only handled, expected failures name a specific
 * error code; everything else collapses to INTERNAL_ERROR.
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