package ro.garajulmeu.user;

import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.garajulmeu.auth.RefreshTokenService;
import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;
import ro.garajulmeu.user.dto.ChangePasswordRequest;
import ro.garajulmeu.user.dto.DeleteAccountRequest;
import ro.garajulmeu.user.dto.UpdateProfileRequest;
import ro.garajulmeu.user.dto.UserProfileResponse;
import ro.garajulmeu.vehicle.VehicleImageService;

@Service
public class UserService {

	private static final Logger log = LoggerFactory.getLogger(UserService.class);

	private final UserRepository userRepository;

	private final PasswordEncoder passwordEncoder;

	private final RefreshTokenService refreshTokenService;

	private final VehicleImageService vehicleImageService;

	UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
			RefreshTokenService refreshTokenService, VehicleImageService vehicleImageService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.refreshTokenService = refreshTokenService;
		this.vehicleImageService = vehicleImageService;
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

	/**
	 * Permanent, per specification section 24. No soft delete, no anonymised
	 * tombstone row: the address becomes free again immediately, which is both
	 * what "delete my account" means to the person asking and what a data
	 * protection request means legally.
	 *
	 * <p>The rows in {@code verification_tokens} and {@code refresh_tokens} go
	 * with it, and <strong>the foreign key is the only thing that removes
	 * them</strong> - neither entity is mapped as an association, so JPA cascades
	 * nothing here. If a future migration drops {@code ON DELETE CASCADE}, this
	 * method would silently leave orphans behind; the test that counts the rows
	 * afterwards is what makes that impossible to miss.
	 *
	 * <p><strong>The photographs are the one thing no foreign key can reach</strong>,
	 * and 12.3b is where that was noticed. The cascade removes the vehicles and
	 * their {@code image_object_key} values, after which nothing in the database
	 * knows where the files are - so the keys are collected first and the objects
	 * removed once the rows are gone. Section 24 makes deletion mean the data is
	 * removed, and a photograph of somebody's car outliving the account that owned
	 * it is exactly what that forbids.
	 *
	 * <p>Sessions are not revoked explicitly because there is nothing left to
	 * revoke: the rows that represent them cease to exist. The access token
	 * already issued stays cryptographically valid until it expires, and opens
	 * nothing - every route resolves the account first and finds none.
	 */
	@Transactional
	public void deleteAccount(UUID accountId, DeleteAccountRequest request) {
		User user = load(accountId);

		if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
			throw new ApiException(ErrorCode.INVALID_CURRENT_PASSWORD);
		}

		// Before the delete, and it has to be: the cascade takes the vehicles and
		// every key they carried, leaving the objects unreachable and undeletable.
		List<String> objectKeys = vehicleImageService.imageKeysOf(accountId);

		userRepository.delete(user);

		// Flushed here rather than left to commit, so the cascade runs inside this
		// method. A foreign key that has lost its cascade then fails on this line,
		// where the cause is obvious, instead of inside Spring's commit machinery.
		userRepository.flush();

		vehicleImageService.discard(objectKeys);

		// The identifier and a count. Logging the address of an account somebody
		// has just asked to erase would defeat the request in the log itself; the
		// count is safe and is the only evidence that the objects were dealt with.
		log.info("Deleted account {} and {} stored image(s)", accountId, objectKeys.size());
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