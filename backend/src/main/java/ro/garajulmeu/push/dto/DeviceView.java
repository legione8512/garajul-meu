package ro.garajulmeu.push.dto;

import java.time.Instant;
import java.util.UUID;

import ro.garajulmeu.push.DevicePlatform;

/**
 * A registered device as the client may see it.
 *
 * <p><strong>No token, in any form.</strong> Section 10.7 excludes it from API
 * responses, and the client that registered it already has it - sending it back
 * would put a live credential in a response body, a browser cache and anything
 * in between, in exchange for nothing.
 */
public record DeviceView(
		UUID id,
		DevicePlatform platform,
		String deviceName,
		boolean notificationsEnabled,
		Instant tokenUpdatedAt) {
}