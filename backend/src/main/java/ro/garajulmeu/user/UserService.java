package ro.garajulmeu.user;

import java.time.ZoneId;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.garajulmeu.auth.RefreshTokenService;
import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;
import ro.garajulmeu.user.dto.ChangePasswordRequest;
import ro.garajulmeu.user.dto.UpdateProfileRequest;
import ro.garajulmeu.user.dto.UserProfileResponse;

@Service
public class UserService {

	private final UserRepository userRepository;

	private final PasswordEncoder passwordEncoder;

	private final RefreshTokenService refreshTokenService;

	UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
			RefreshTokenService refreshTokenService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.refreshTokenService = refreshTokenService;
	}

	/**
	 * The caller can only ever pass the id from their own token, so this needs no
	 * ownership check. Every resource that belongs to somebody else will.
	 */
	@Transactional(readOnly = true)
	public UserProfileResponse profileOf(UUID accountId) {
		return profileFrom(load(accountId));
	}

	/**
	 * Absent means unchanged. A field the client never sends must not be reset to
	 * null, which is the whole difference between PATCH and PUT.
	 */
	@Transactional
	public UserProfileResponse updateProfile(UUID accountId, UpdateProfileRequest request) {
		User user = load(accountId);

		if (request.fullName() != null) {
			user.setFullName(request.fullName().trim());
		}

		if (request.preferredLanguage() != null) {
			user.setPreferredLanguage(Language.fromCode(request.preferredLanguage()));
		}

		if (request.timezone() != null) {
			user.setTimezone(validZone(request.timezone()));
		}

		return profileFrom(user);
	}

	/**
	 * Ends every session, including the one that asked. Section 14 requires it
	 * after a reset for a reason that applies just as much here: if the password
	 * is being changed because somebody else learned it, their sessions must not
	 * outlive it. Keeping the caller's own session alive would need the access
	 * token to carry its token family, which it deliberately does not.
	 */
	@Transactional
	public void changePassword(UUID accountId, ChangePasswordRequest request) {
		User user = load(accountId);

		if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
			throw new ApiException(ErrorCode.INVALID_CURRENT_PASSWORD);
		}

		user.setPasswordHash(passwordEncoder.encode(request.newPassword()));

		// Last, deliberately. revokeAllForUser is a bulk update: it flushes the
		// new hash to the database and then detaches every loaded entity, so
		// nothing may touch `user` after this line.
		refreshTokenService.revokeAllSessionsOf(accountId);
	}

	private User load(UUID accountId) {
		return userRepository.findById(accountId)
				.orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
	}

	/**
	 * Bean Validation cannot check membership of a set resolved at runtime without
	 * a custom constraint, so the check lives here. The cost is that the response
	 * carries no field name - acceptable while this endpoint has exactly one field
	 * that can produce a VALIDATION_ERROR without one.
	 */
	private static String validZone(String candidate) {
		if (!ZoneId.getAvailableZoneIds().contains(candidate)) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR);
		}
		return candidate;
	}

	private static UserProfileResponse profileFrom(User user) {
		return new UserProfileResponse(
				user.getId(),
				user.getFullName(),
				user.getEmail(),
				user.getPreferredLanguage().code(),
				user.getTimezone(),
				user.isEmailVerified());
	}
}