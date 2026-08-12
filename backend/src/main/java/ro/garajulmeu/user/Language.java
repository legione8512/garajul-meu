package ro.garajulmeu.user;

import java.util.Arrays;

/**
 * The languages the product supports, per specification section 6.
 *
 * <p>The code is the lower-case IETF tag also used by i18next on the frontend
 * and by the transactional email templates, so one value travels unchanged from
 * the database to the rendered message.
 */
public enum Language {

	RO("ro"),
	EN("en");

	private final String code;

	Language(String code) {
		this.code = code;
	}

	public String code() {
		return code;
	}

	public static Language fromCode(String code) {
		return Arrays.stream(values())
				.filter(language -> language.code.equals(code))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Unsupported language code"));
	}
}