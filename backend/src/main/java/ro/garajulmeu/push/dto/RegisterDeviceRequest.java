package ro.garajulmeu.push.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import ro.garajulmeu.push.DevicePlatform;

/**
 * What the native app sends at every start.
 *
 * <p>{@code platform} is the enum rather than text, unlike {@code DocumentType}
 * on a document. There the sender was a person choosing from a list, and being
 * told precisely which field was wrong was worth a code of its own; here the
 * sender is our own client passing a constant, so an unrecognised value is a
 * client defect and Jackson refusing the body says so accurately enough.
 */
public record RegisterDeviceRequest(

		@NotNull DevicePlatform platform,

		@NotBlank String pushToken,

		@Size(max = 120) String deviceName) {
}