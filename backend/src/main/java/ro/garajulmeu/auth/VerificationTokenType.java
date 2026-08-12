package ro.garajulmeu.auth;

/**
 * The three purposes a six-digit code can serve, per specification section 10.10.
 *
 * <p>Codes are never interchangeable between purposes: a password-reset code
 * must not verify an email address. The type is part of every lookup.
 */
public enum VerificationTokenType {

	EMAIL_VERIFICATION,
	PASSWORD_RESET,
	EMAIL_CHANGE
}