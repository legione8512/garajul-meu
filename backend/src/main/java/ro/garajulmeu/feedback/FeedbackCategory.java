package ro.garajulmeu.feedback;

import java.util.Locale;
import java.util.Optional;

/** What a message from the Sugestii tab is about. */
public enum FeedbackCategory {

	IDEA,

	PROBLEM,

	/** A function somebody wishes the application had. */
	FEATURE;

	/** Case-insensitive, and empty rather than an exception for anything unknown. */
	public static Optional<FeedbackCategory> of(String raw) {
		if (raw == null) {
			return Optional.empty();
		}

		String cleaned = raw.trim().toUpperCase(Locale.ROOT);

		for (FeedbackCategory category : values()) {
			if (category.name().equals(cleaned)) {
				return Optional.of(category);
			}
		}
		return Optional.empty();
	}
}
