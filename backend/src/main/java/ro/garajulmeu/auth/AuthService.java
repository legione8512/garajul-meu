package ro.garajulmeu.auth;

import java.time.Instant;
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

	AuthService(UserRepository userRepository, VerificationTokenRepository tokenRepository,
			PasswordEncoder passwordEncoder, EmailProvider emailProvider,
			VerificationCodeGenerator codeGenerator, AuthProperties authProperties) {
		this.userRepository = userRepository;
		this.tokenRepository = tokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.emailProvider = emailProvider;
		this.codeGenerator = codeGenerator;
		this.authProperties = authProperties;
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

		issueEmailVerificationCode(user);

		// The identifier only. The address is personal data and adds nothing here.
		log.info("Registered account {}", user.getId());
	}

	private void issueEmailVerificationCode(User user) {
		Instant now = Instant.now();
		tokenRepository.invalidateOutstandingCodes(user.getId(), VerificationTokenType.EMAIL_VERIFICATION, now);

		String code = codeGenerator.generate();
		tokenRepository.save(new VerificationToken(
				user.getId(),
				VerificationTokenType.EMAIL_VERIFICATION,
				passwordEncoder.encode(code),
				now.plus(authProperties.verificationCodeValidity())));

		emailProvider.sendVerificationCode(user.getEmail(), code, user.getPreferredLanguage());
	}

	/**
	 * Trimmed and lower-cased, so the same person cannot hold two accounts that
	 * differ only in capitalisation, and so the unique index sees one form.
	 */
	private static String normalise(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private static Language languageOf(String code) {
		return code == null ? Language.RO : Language.fromCode(code);
	}
}