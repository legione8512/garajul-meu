package ro.garajulmeu.push;

/**
 * Where a registration came from. Specification section 10.7.
 *
 * <p>Two values and no WEB, deliberately: section 18 states that V1 does not
 * implement Firebase Web Push and that this table holds native registrations
 * only. A third constant would be an invitation to store something the delivery
 * path cannot reach.
 */
public enum DevicePlatform {
	IOS,
	ANDROID
}