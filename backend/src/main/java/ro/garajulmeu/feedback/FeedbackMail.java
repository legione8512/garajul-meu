package ro.garajulmeu.feedback;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * The email the operator receives for one message. Always in Romanian, because
 * it is read by one person, who reads Romanian - unlike the account's own
 * emails, which follow the reader.
 *
 * <p>The sender is named and their address is the email's Reply-To, so
 * answering is one button in the operator's mail client and the person's
 * address never has to be copied anywhere.
 */
final class FeedbackMail {

	private static final ZoneId BUCHAREST = ZoneId.of("Europe/Bucharest");

	private static final DateTimeFormatter SENT =
			DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.ROOT).withZone(BUCHAREST);

	/** Long enough to recognise a message in an inbox, short enough for a subject line. */
	static final int SUBJECT_EXCERPT = 60;

	private FeedbackMail() {
	}

	record Mail(String subject, String body) {
	}

	static Mail of(FeedbackCategory category, String message, String senderName, String senderEmail,
			String platform, String appVersion, Instant sentAt) {
		String subject = "Garajul Meu — " + label(category) + ": " + excerpt(message);

		String body = """
				Tip: %s
				De la: %s <%s>
				Platformă: %s
				Trimis: %s (ora României)

				%s

				---
				Răspunde la acest email ca să-i scrii direct utilizatorului.
				""".formatted(label(category), senderName, senderEmail,
				running(platform, appVersion), SENT.format(sentAt), message);

		return new Mail(subject, body);
	}

	static String label(FeedbackCategory category) {
		return switch (category) {
			case IDEA -> "Idee";
			case PROBLEM -> "Problemă";
			case FEATURE -> "Funcție dorită";
		};
	}

	/** The first line of the message, cut at a word where it can be. */
	static String excerpt(String message) {
		String line = message.strip().lines().findFirst().orElse("").strip();

		if (line.length() <= SUBJECT_EXCERPT) {
			return line;
		}

		String cut = line.substring(0, SUBJECT_EXCERPT);
		int space = cut.lastIndexOf(' ');
		return (space > SUBJECT_EXCERPT / 2 ? cut.substring(0, space) : cut) + "…";
	}

	private static String running(String platform, String appVersion) {
		if (platform == null) {
			return "necunoscută";
		}

		String name = switch (platform) {
			case "ANDROID" -> "Android";
			case "IOS" -> "iOS";
			default -> "Web";
		};
		return appVersion == null ? name : name + " " + appVersion;
	}
}
