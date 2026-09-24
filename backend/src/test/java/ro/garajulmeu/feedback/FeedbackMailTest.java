package ro.garajulmeu.feedback;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FeedbackMailTest {

	private static final Instant SENT = Instant.parse("2026-09-23T16:40:00Z");

	@Test
	void theSubjectNamesTheKindAndTheStartOfTheMessage() {
		FeedbackMail.Mail mail = FeedbackMail.of(FeedbackCategory.FEATURE,
				"Aș vrea să pot adăuga și revizia", "Ana Pop", "ana@example.com", "ANDROID", "1.1.0",
				SENT);

		assertThat(mail.subject()).isEqualTo("Garajul Meu — Funcție dorită: Aș vrea să pot adăuga și revizia");
	}

	@Test
	void theBodyCarriesTheSenderThePlatformTheTimeAndTheMessage() {
		FeedbackMail.Mail mail = FeedbackMail.of(FeedbackCategory.PROBLEM,
				"Nu primesc notificări.\nAm Android 9.", "Ana Pop", "ana@example.com", "ANDROID",
				"1.1.0", SENT);

		assertThat(mail.body())
				.contains("Tip: Problemă")
				.contains("De la: Ana Pop <ana@example.com>")
				.contains("Platformă: Android 1.1.0")
				// 16:40 UTC is 19:40 in Bucharest in September.
				.contains("Trimis: 23.09.2026 19:40 (ora României)")
				.contains("Nu primesc notificări.\nAm Android 9.");
	}

	@Test
	void anUnknownPlatformAndTheWebAreSaidPlainly() {
		assertThat(FeedbackMail.of(FeedbackCategory.IDEA, "x", "A", "a@example.com", null, null, SENT)
				.body()).contains("Platformă: necunoscută");
		assertThat(FeedbackMail.of(FeedbackCategory.IDEA, "x", "A", "a@example.com", "WEB", null, SENT)
				.body()).contains("Platformă: Web\n");
	}

	/** Only the first line, cut at a word, so an inbox shows something readable. */
	@Test
	void aLongMessageIsCutAtAWordForTheSubject() {
		String excerpt = FeedbackMail.excerpt(
				"Ar fi foarte util dacă aplicația ar putea să îmi arate și costurile totale pe an\nrest");

		assertThat(excerpt).endsWith("…");
		assertThat(excerpt.length()).isLessThanOrEqualTo(FeedbackMail.SUBJECT_EXCERPT + 1);
		assertThat(excerpt).doesNotContain("rest");
		assertThat(FeedbackMail.excerpt("  scurt  ")).isEqualTo("scurt");
	}
}
