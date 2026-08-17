package ro.garajulmeu.push;

import java.util.Map;

/**
 * What arrives on a phone. Specification section 36 names the seam this travels
 * through; this is the part that carries no provider vocabulary at all.
 *
 * @param data identifiers only - a vehicle and a document - so tapping the
 *             notification can open the right screen. Never a policy number,
 *             never an address: a notification payload is stored by the operating
 *             system and shown on a locked screen, which makes it the least
 *             private place in the system.
 */
public record PushNotification(String title, String body, Map<String, String> data) {
}