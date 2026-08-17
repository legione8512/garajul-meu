package ro.garajulmeu.push;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Writes the notification to the log instead of delivering it.
 *
 * <p>Active for the whole of V1 web, not merely in development: section 18 makes
 * push native-only and the apps arrive in phases 17 and 18, so there is nothing
 * to deliver to yet. That is a longer life than LoggingEmailProvider was ever
 * meant to have, which is why the warning is worded as it is.
 *
 * <p>Selected by an explicit property, as the other two seams are, so production
 * cannot fall back to it silently - with the property absent there is no
 * PushNotificationProvider bean and the application refuses to start.
 *
 * <p><strong>The token is written as a fragment, never whole.</strong> Section 27
 * lists full push tokens among the things that must never reach a log; the last
 * few characters tell two devices apart and cannot address either.
 */
@Component
@ConditionalOnProperty(name = "garajul-meu.push.provider", havingValue = "logging")
public class LoggingPushNotificationProvider implements PushNotificationProvider {

	private static final Logger log = LoggerFactory.getLogger(LoggingPushNotificationProvider.class);

	private static final int FINGERPRINT_LENGTH = 6;

	public LoggingPushNotificationProvider() {
		log.warn("Push provider is LOGGING: notifications are written to this log, not delivered. "
				+ "Expected until the native applications exist; must never be the case after.");
	}

	@Override
	public void send(String pushToken, PushNotification notification) {
		log.info("Push to device {} — {} / {} — data {}",
				fingerprint(pushToken), notification.title(), notification.body(),
				notification.data().keySet());
	}

	/** Keys of the payload, not values: the keys are field names, the values are ours. */
	private static String fingerprint(String pushToken) {
		if (pushToken == null || pushToken.length() <= FINGERPRINT_LENGTH) {
			return "…";
		}
		return "…" + pushToken.substring(pushToken.length() - FINGERPRINT_LENGTH);
	}
}