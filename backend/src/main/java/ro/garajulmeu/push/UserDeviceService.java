package ro.garajulmeu.push;

import java.time.Clock;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;
import ro.garajulmeu.push.dto.DeviceView;
import ro.garajulmeu.push.dto.RegisterDeviceRequest;

@Service
public class UserDeviceService {

	private static final Logger log = LoggerFactory.getLogger(UserDeviceService.class);

	private final UserDeviceRepository deviceRepository;
	private final Clock clock;

	UserDeviceService(UserDeviceRepository deviceRepository, Clock clock) {
		this.deviceRepository = deviceRepository;
		this.clock = clock;
	}

	/**
	 * Registers this installation, or updates the registration it already has.
	 *
	 * <p><strong>A token found under another account is moved, not refused.</strong>
	 * An FCM token identifies one installation of the app rather than one person:
	 * if somebody signs out of a phone and somebody else signs in, the handset must
	 * stop receiving the first account's reminders. Refusing would leave the old
	 * owner reachable on a device they no longer hold, and a second row is
	 * impossible anyway - the unique index says a token belongs to one place.
	 *
	 * <p>Idempotent by design, because the native client calls this on every
	 * launch: the same token twice updates {@code last_seen_at} and nothing else
	 * of consequence.
	 */
	@Transactional
	public DeviceView register(UUID accountId, RegisterDeviceRequest request) {
		UserDevice device = deviceRepository.findByPushToken(request.pushToken())
				.orElseGet(() -> new UserDevice(accountId, request.platform(), request.pushToken()));

		boolean moved = !accountId.equals(device.getUserId());

		device.setUserId(accountId);
		device.setPlatform(request.platform());
		device.setDeviceName(trimmedOrNull(request.deviceName()));
		device.setLastSeenAt(clock.instant());

		deviceRepository.saveAndFlush(device);

		if (moved) {
			log.info("Device {} moved to account {}", device.fingerprint(), accountId);
		} else {
			log.info("Registered device {} for account {}", device.fingerprint(), accountId);
		}

		return view(device);
	}

	/**
	 * Unregisters one device of this account. A device belonging to somebody else
	 * answers exactly as one that does not exist, per section 15.
	 */
	@Transactional
	public void unregister(UUID accountId, UUID deviceId) {
		UserDevice device = deviceRepository.findByIdAndUserId(deviceId, accountId)
				.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));

		deviceRepository.delete(device);
		deviceRepository.flush();

		log.info("Unregistered device {} of account {}", device.fingerprint(), accountId);
	}

	private static String trimmedOrNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static DeviceView view(UserDevice device) {
		return new DeviceView(
				device.getId(),
				device.getPlatform(),
				device.getDeviceName(),
				device.isNotificationsEnabled(),
				device.getTokenUpdatedAt());
	}
}