package ro.garajulmeu.exception;

import org.springframework.http.HttpStatus;

/**
 * The canonical catalogue of machine-readable error codes, per specification
 * section 17.
 *
 * <p>Every failed API response carries exactly one of these. The frontend maps
 * the code to a Romanian or English message; the backend never sends
 * user-facing text. New codes belong here and nowhere else - a code invented
 * inline in a controller cannot be translated, because the frontend has no
 * i18n key for it.
 *
 * <p>The HTTP status lives with the code so the same failure can never answer
 * 404 in one endpoint and 400 in another.
 */
public enum ErrorCode {

	// Authentication
	/**
	 * No bearer token, or one that failed verification. Deliberately one code for
	 * both: the client's next move - refresh, then log in - is the same either
	 * way, so a second code would be a distinction nobody consumes.
	 */
	AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED),
	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
	EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN),
	REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED),
	REFRESH_TOKEN_REUSED(HttpStatus.UNAUTHORIZED),
	VERIFICATION_CODE_INVALID(HttpStatus.BAD_REQUEST),
	VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST),

	// User
	EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT),
	USER_NOT_FOUND(HttpStatus.NOT_FOUND),
	INVALID_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST),

	// Vehicle
	VEHICLE_NOT_FOUND(HttpStatus.NOT_FOUND),
	VEHICLE_ACCESS_DENIED(HttpStatus.FORBIDDEN),
	VEHICLE_DUPLICATE_VIN(HttpStatus.CONFLICT),

	// Registration certificate
	REGISTRATION_CERTIFICATE_NOT_FOUND(HttpStatus.NOT_FOUND),
	REGISTRATION_CERTIFICATE_INVALID_FIELD(HttpStatus.BAD_REQUEST),

	// Vehicle documents
	DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND),
	DOCUMENT_INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST),
	DOCUMENT_TYPE_INVALID(HttpStatus.BAD_REQUEST),

	// OCR
	OCR_FILE_INVALID(HttpStatus.BAD_REQUEST),
	OCR_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS),
	OCR_PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),
	OCR_PROCESSING_FAILED(HttpStatus.UNPROCESSABLE_CONTENT),

	// Storage
	IMAGE_INVALID_TYPE(HttpStatus.BAD_REQUEST),
	IMAGE_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE),
	STORAGE_PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),

	// General
	VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
	MALFORMED_REQUEST(HttpStatus.BAD_REQUEST),
	RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED),
	RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

	private final HttpStatus status;

	ErrorCode(HttpStatus status) {
		this.status = status;
	}

	public HttpStatus status() {
		return status;
	}
}