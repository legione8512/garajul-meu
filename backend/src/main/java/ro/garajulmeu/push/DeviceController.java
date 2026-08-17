package ro.garajulmeu.push;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ro.garajulmeu.push.dto.DeviceView;
import ro.garajulmeu.push.dto.RegisterDeviceRequest;

/**
 * Specification section 16's two device endpoints. Nothing calls them in V1: the
 * only clients that can register are the native applications of phases 17 and 18.
 */
@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {

	private final UserDeviceService deviceService;

	DeviceController(UserDeviceService deviceService) {
		this.deviceService = deviceService;
	}

	/**
	 * 200 rather than 201, because this is an upsert called at every app launch
	 * and the usual answer is "the registration you already had". A 201 would
	 * claim something was created almost every time it was not.
	 */
	@PostMapping
	public DeviceView register(@AuthenticationPrincipal Jwt token,
			@Valid @RequestBody RegisterDeviceRequest request) {
		return deviceService.register(accountOf(token), request);
	}

	@DeleteMapping("/{deviceId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unregister(@AuthenticationPrincipal Jwt token, @PathVariable UUID deviceId) {
		deviceService.unregister(accountOf(token), deviceId);
	}

	private static UUID accountOf(Jwt token) {
		return UUID.fromString(token.getSubject());
	}
}