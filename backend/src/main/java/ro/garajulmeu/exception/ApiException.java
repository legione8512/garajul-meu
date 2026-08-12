package ro.garajulmeu.exception;

/**
 * Thrown for every expected business failure.
 *
 * <p>It carries an {@link ErrorCode}, never a user-facing message: the wording
 * shown to a person is chosen by the frontend so it can be Romanian or English.
 *
 * <p>The exception message is set to the code name purely so server logs and
 * stack traces stay readable. It is never sent to a client.
 */
public class ApiException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final ErrorCode errorCode;

	public ApiException(ErrorCode errorCode) {
		super(errorCode.name());
		this.errorCode = errorCode;
	}

	public ApiException(ErrorCode errorCode, Throwable cause) {
		super(errorCode.name(), cause);
		this.errorCode = errorCode;
	}

	public ErrorCode errorCode() {
		return errorCode;
	}
}