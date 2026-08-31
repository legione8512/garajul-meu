package ro.garajulmeu.email;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

import ro.garajulmeu.auth.AuthProperties;
import ro.garajulmeu.user.Language;

/**
 * No Spring context: the templates are a pure function of a code, a language
 * and one duration.
 */
class EmailMessagesTest {

	private static final Duration VALIDITY = Duration.ofMinutes(15);

	private final EmailMessages messages = new EmailMessages(
			new AuthProperties(VALIDITY, 5, Duration.ofDays(30), Duration.ofSeconds(15)));

	/** Each purpose, as a function of a code and a language. */
	private List<BiFunction<String, Language, EmailMessages.Message>> everyPurpose() {
		return List.of(messages::verification, messages::passwordReset,
				(code, language) -> messages.emailChange("nou@example.com", code, language));
	}

	/**
	 * Derived over {@code Language.values()} rather than written out, so adding a
	 * third language fails here on the day it is added instead of silently sending
	 * Romanian to somebody who asked for something else. The switch expressions are
	 * exhaustive, so a missing case is a compile error - this catches the other
	 * half: a case that exists and produces nothing.
	 */
	@Test
	void everyPurposeSpeaksEveryLanguage() {
		List<String> empty = new ArrayList<>();

		for (Language language : Language.values()) {
			for (BiFunction<String, Language, EmailMessages.Message> purpose : everyPurpose()) {
				EmailMessages.Message message = purpose.apply("123456", language);

				if (message.subject().isBlank() || message.body().isBlank()) {
					empty.add(language + ": " + message.subject());
				}
			}
		}

		assertThat(empty).as("messages with an empty subject or body").isEmpty();
	}

	@Test
	void everyMessageCarriesTheCodeItWasAskedToDeliver() {
		for (Language language : Language.values()) {
			for (BiFunction<String, Language, EmailMessages.Message> purpose : everyPurpose()) {
				assertThat(purpose.apply("602431", language).body()).as("the body in %s", language).contains("602431");
			}
		}
	}

	/**
	 * The requested address is named, because the reader who did not ask for the
	 * change is the reader this message exists for. Without it they are told
	 * something is happening and not what.
	 */
	@Test
	void theEmailChangeNamesTheAddressSomebodyIsMovingTheAccountTo() {
		for (Language language : Language.values()) {
			assertThat(messages.emailChange("altcineva@example.com", "123456", language).body())
					.as("the email-change body in %s", language).contains("altcineva@example.com");
		}
	}

	/**
	 * The sentence has to follow the setting. A template that hardcoded fifteen
	 * would keep saying fifteen after somebody configured thirty, and the lie would
	 * be told to every user and caught by nobody.
	 */
	@Test
	void theStatedValidityFollowsTheConfiguredOne() {
		EmailMessages thirty = new EmailMessages(
				new AuthProperties(Duration.ofMinutes(30), 5, Duration.ofDays(30), Duration.ofSeconds(15)));

		assertThat(messages.verification("123456", Language.RO).body()).contains("15 minute");
		assertThat(thirty.verification("123456", Language.RO).body()).contains("30 minute");
	}

	/**
	 * Guards against the quiet failure where one language's branch was copied and
	 * its text never translated - which produces a message that is perfectly valid,
	 * perfectly readable, and in the wrong language.
	 */
	@Test
	void theLanguagesActuallyDiffer() {
		for (BiFunction<String, Language, EmailMessages.Message> purpose : everyPurpose()) {
			EmailMessages.Message romanian = purpose.apply("123456", Language.RO);
			EmailMessages.Message english = purpose.apply("123456", Language.EN);

			assertThat(romanian.subject()).isNotEqualTo(english.subject());
			assertThat(romanian.body()).isNotEqualTo(english.body());
		}
	}
}