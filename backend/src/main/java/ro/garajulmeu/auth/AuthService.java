package ro.garajulmeu.auth;

import java.time.Instant;
import ro.garajulmeu.security.AccessTokenService.IssuedAccessToken;

import java.time.Duration;
import java.util.UUID;

import ro.garajulmeu.auth.dto.ForgotPasswordRequest;
import ro.garajulmeu.auth.dto.LoginRequest;
import ro.garajulmeu.auth.dto.ResetPasswordRequest;
import ro.garajulmeu.security.AccessTokenService;
import java.util.Optional;

import ro.garajulmeu.auth.dto.ResendVerificationRequest;
import ro.garajulmeu.auth.dto.VerifyEmailRequest;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.garajulmeu.auth.dto.RegisterRequest;
import ro.garajulmeu.email.EmailProvider;
import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;
import ro.garajulmeu.user.Language;
import ro.garajulmeu.user.User;
import ro.garajulmeu.user.UserRepository;

@Service
public class AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);

	private final UserRepository userRepository;

	private final VerificationTokenRepository tokenRepository;

	private final PasswordEncoder passwordEncoder;

	private final EmailProvider emailProvider;

	private final VerificationCodeGenerator codeGenerator;

	private final AuthProperties authProperties;
	
	private final AccessTokenService accessTokenService;
	
	private final RefreshTokenService refreshTokenService;

	AuthService(UserRepository userRepository, VerificationTokenRepository tokenRepository,
			PasswordEncoder passwordEncoder, EmailProvider emailProvider,
			VerificationCodeGenerator codeGenerator, AuthProperties authProperties,
			AccessTokenService accessTokenService, RefreshTokenService refreshTokenService) {
		this.userRepository = userRepository;
		this.tokenRepository = tokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.emailProvider = emailProvider;
		this.codeGenerator = codeGenerator;
		this.authProperties = authProperties;
		this.accessTokenService = accessTokenService;
		this.refreshTokenService = refreshTokenService;
	}

	@Transactional
	public void register(RegisterRequest request) {
		String email = normalise(request.email());

		if (userRepository.existsByEmail(email)) {
			throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
		}

		User user = new User(request.fullName().trim(), email, passwordEncoder.encode(request.password()));
		user.setPreferredLanguage(languageOf(request.preferredLanguage()));

		try {
			userRepository.saveAndFlush(user);
		}
		catch (DataIntegrityViolationException duplicate) {
			// Two simultaneous registrations for the same address can both pass the
			// check above and collide only at the unique index. The database is the
			// arbiter; without this the caller would receive INTERNAL_ERROR for a
			// perfectly ordinary conflict.
			throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS, duplicate);
		}

		emailProvider.sendVerificationCode(user.getEmail(),
				issueCode(user, VerificationTokenType.EMAIL_VERIFICATION),
				user.getPreferredLanguage());

		// The identifier only. The address is personal data and adds nothing here.
		log.info("Registered account {}", user.getId());
	}

	/**
	 * {@code noRollbackFor} is essential, not cosmetic. A business exception
	 * normally rolls the transaction back, which would discard the very thing we
	 * just recorded - the failed attempt. The attempt counter would stay at zero
	 * forever and the limit that makes a six-digit code safe would never trigger.
	 */
	@Transactional(noRollbackFor = ApiException.class)
	public void verifyEmail(VerifyEmailRequest request) {
		User user = userRepository.findByEmail(normalise(request.email()))
				.orElseThrow(() -> new ApiException(ErrorCode.VERIFICATION_CODE_INVALID));

		Instant now = Instant.now();
		consumeCode(user.getId(), VerificationTokenType.EMAIL_VERIFICATION, request.code(), now);

		user.setEmailVerifiedAt(now);
		log.info("Verified account {}", user.getId());
	}

	/**
	 * Answers identically for an unknown address, an already verified one and a
	 * successful reissue. Anything else would turn this endpoint into a way of
	 * discovering which addresses hold accounts.
	 */
	@Transactional
	public void resendVerificationCode(ResendVerificationRequest request) {
		Optional<User> account = userRepository.findByEmail(normalise(request.email()));

		if (account.isEmpty() || account.get().isEmailVerified()) {
			log.info("Verification resend requested for an unknown or already verified address");
			return;
		}

		User user = account.get();
		emailProvider.sendVerificationCode(user.getEmail(),
				issueCode(user, VerificationTokenType.EMAIL_VERIFICATION),
				user.getPreferredLanguage());
		log.info("Reissued verification code for account {}", user.getId());
	}

	/**
	 * Answers 204 whether or not the address exists. Specification section 14
	 * requires non-disclosure here specifically: unlike registration, where
	 * EMAIL_ALREADY_EXISTS is a defined outcome, this endpoint needs no account
	 * holder's cooperation, so a truthful answer would be a free membership
	 * oracle for anyone with a list of addresses.
	 */
	@Transactional
	public void forgotPassword(ForgotPasswordRequest request) {
		Optional<User> account = userRepository.findByEmail(normalise(request.email()));

		if (account.isEmpty()) {
			log.info("Password reset requested for an unknown address");
			return;
		}

		User user = account.get();
		emailProvider.sendPasswordResetCode(user.getEmail(),
				issueCode(user, VerificationTokenType.PASSWORD_RESET),
				user.getPreferredLanguage());
		log.info("Issued password reset code for account {}", user.getId());
	}

	/** {@code noRollbackFor} for the same reason as {@link #verifyEmail}. */
	@Transactional(noRollbackFor = ApiException.class)
	public void resetPassword(ResetPasswordRequest request) {
		User user = userRepository.findByEmail(normalise(request.email()))
				.orElseThrow(() -> new ApiException(ErrorCode.VERIFICATION_CODE_INVALID));

		Instant now = Instant.now();
		consumeCode(user.getId(), VerificationTokenType.PASSWORD_RESET, request.code(), now);

		user.setPasswordHash(passwordEncoder.encode(request.newPassword()));

		// Entering a code that arrived by email proves control of the inbox, which
		// is exactly what verification asks for. Without this, an account that was
		// never verified could reset successfully and still be refused at login
		// with EMAIL_NOT_VERIFIED, and nothing on screen would explain why.
		if (!user.isEmailVerified()) {
			user.setEmailVerifiedAt(now);
			log.info("Password reset also verified the address of account {}", user.getId());
		}

		// Last, deliberately. revokeAllForUser is a bulk update: it flushes the
		// changes above to the database and then detaches every loaded entity, so
		// nothing may touch `user` or the token after this line.
		refreshTokenService.revokeAllSessionsOf(user.getId());

		log.info("Password reset for account {}", user.getId());
	}

	/**
	 * No longer {@code readOnly}: issuing a refresh token writes a row. A
	 * read-only transaction would have refused the insert.
	 */
	@Transactional
	public LoginResult login(LoginRequest request) {
		Optional<User> account = userRepository.findByEmail(normalise(request.email()));

		if (account.isEmpty()) {
			// Hash a throwaway value so a missing account costs the same as a wrong
			// password. Without this, response time alone tells an attacker which
			// addresses hold accounts.
			passwordEncoder.encode(request.password());
			throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
		}

		User user = account.get();

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
		}

		// Deliberately after the password check, so an unverified account is only
		// revealed to someone who already proved they know the password.
		if (!user.isEmailVerified()) {
			throw new ApiException(ErrorCode.EMAIL_NOT_VERIFIED);
		}

		IssuedAccessToken access = accessTokenService.issueFor(user.getId());
		RefreshTokenService.IssuedRefreshToken refresh = refreshTokenService.startFamily(user.getId());

		log.info("Started session for account {} in family {}", user.getId(), refresh.familyId());

		return new LoginResult(access.value(), secondsUntil(access.expiresAt()), refresh.value());
	}

	@Transactional
	public LoginResult refresh(String presentedToken) {
		RefreshTokenService.IssuedRefreshToken rotated = refreshTokenService.rotate(presentedToken);
		IssuedAccessToken access = accessTokenService.issueFor(rotated.userId());

		return new LoginResult(access.value(), secondsUntil(access.expiresAt()), rotated.value());
	}

	@Transactional
	public void logout(String presentedToken) {
		refreshTokenService.revokeSessionOf(presentedToken);
	}

	private static long secondsUntil(Instant moment) {
		return Duration.between(Instant.now(), moment).toSeconds();
	}

	/** What the service produces; the controller decides how it travels. */
	public record LoginResult(String accessToken, long expiresInSeconds, String refreshToken) {
	}

	/**
	 * The single place a six-digit code is checked, for every purpose.
	 *
	 * <p>Verification and password reset need the identical sequence - spent,
	 * expired, too many attempts, wrong - and writing it twice is precisely how
	 * two flows end up with quietly different rules. The caller supplies the type,
	 * because a code issued for one purpose must never open another.
	 */
	private void consumeCode(UUID userId, VerificationTokenType type, String presentedCode, Instant now) {
		VerificationToken token = tokenRepository
				.findFirstByUserIdAndTypeOrderByCreatedAtDesc(userId, type)
				.orElseThrow(() -> new ApiException(ErrorCode.VERIFICATION_CODE_INVALID));

		if (token.getUsedAt() != null || token.getInvalidatedAt() != null) {
			throw new ApiException(ErrorCode.VERIFICATION_CODE_INVALID);
		}

		if (!token.getExpiresAt().isAfter(now)) {
			throw new ApiException(ErrorCode.VERIFICATION_CODE_EXPIRED);
		}

		if (token.getAttemptCount() >= authProperties.maxVerificationAttempts()) {
			token.markInvalidated(now);
			log.info("Burnt {} code for account {} after too many attempts", type, userId);
			throw new ApiException(ErrorCode.VERIFICATION_CODE_INVALID);
		}

		if (!passwordEncoder.matches(presentedCode, token.getTokenHash())) {
			token.recordFailedAttempt();
			log.info("Wrong {} code for account {}, attempt {}", type, userId, token.getAttemptCount());
			throw new ApiException(ErrorCode.VERIFICATION_CODE_INVALID);
		}

		token.markUsed(now);
	}

	/** Supersedes any outstanding code of this type and returns the new one. */
	private String issueCode(User user, VerificationTokenType type) {
		Instant now = Instant.now();
		tokenRepository.invalidateOutstandingCodes(user.getId(), type, now);

		String code = codeGenerator.generate();
		tokenRepository.save(new VerificationToken(
				user.getId(),
				type,
				passwordEncoder.encode(code),
				now.plus(authProperties.verificationCodeValidity())));

		return code;
	}

	/**
	 * Trimmed and lower-cased, so the same person cannot hold two accounts that
	 * differ only in capitalisation, and so the unique index sees one form.
	 */
	static String normalise(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private static Language languageOf(String code) {
		return code == null ? Language.RO : Language.fromCode(code);
	}
}