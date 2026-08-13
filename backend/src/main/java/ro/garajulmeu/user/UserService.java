package ro.garajulmeu.user;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;
import ro.garajulmeu.user.dto.UserProfileResponse;

@Service
public class UserService {

	private final UserRepository userRepository;

	UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	/**
	 * The caller can only ever pass the id from their own token, so this needs no
	 * ownership check. Every resource that belongs to somebody else will.
	 */
	@Transactional(readOnly = true)
	public UserProfileResponse profileOf(UUID accountId) {
		User user = userRepository.findById(accountId)
				.orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

		return new UserProfileResponse(
				user.getId(),
				user.getFullName(),
				user.getEmail(),
				user.getPreferredLanguage().code(),
				user.getTimezone(),
				user.isEmailVerified());
	}
}