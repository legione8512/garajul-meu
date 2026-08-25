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

		@Size(max = 120) String deviceName,

		/**
		 * Whether this installation can currently show a notification at all -
		 * which on both platforms means the operating system's own permission,
		 * reported afresh on every launch.
		 *
		 * <p><strong>The device-level switch V9 describes has no screen of ours,
		 * and it does not need one: the operating system is that screen.</strong>
		 * Somebody who silences one phone does it in Android's notification
		 * settings, which is exactly "one phone silenced is not the account
		 * silenced" - the account-level switch lives in
		 * {@code notification_preferences} and is written by screen 15.
		 *
		 * <p>The failure this closes is a lie rather than an inconvenience. A
		 * permission revoked after registration leaves the token perfectly valid,
		 * so FCM accepts the message, the dispatcher records the reminder as SENT,
		 * and the person is shown nothing. Reporting the truth at every launch
		 * costs no extra request, because the native client already calls this
		 * endpoint every time it starts.
		 *
		 * <p>Required, deliberately. A client that omits it must fail validation
		 * loudly; an optional field defaulting to true would reintroduce exactly
		 * the silent wrong answer this exists to prevent.
		 */
		@NotNull Boolean notificationsEnabled) {
}