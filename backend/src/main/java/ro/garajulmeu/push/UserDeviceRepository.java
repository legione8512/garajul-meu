package ro.garajulmeu.push;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDeviceRepository extends JpaRepository<UserDevice, UUID> {

	/**
	 * By the token's hash alone, with no account id - the one finder in this
	 * project that is deliberately unscoped. A token identifies an installation
	 * rather than an account, and registration has to be able to find one
	 * currently attached to somebody else in order to move it.
	 *
	 * <p>By the hash rather than the token, because the token column is
	 * encrypted and an authenticated cipher writes different bytes every time.
	 * A finder on the value would compile, run, and never match anything.
	 */
	Optional<UserDevice> findByPushTokenHash(String pushTokenHash);

	Optional<UserDevice> findByIdAndUserId(UUID id, UUID userId);

	List<UserDevice> findByUserIdAndNotificationsEnabledTrue(UUID userId);
}