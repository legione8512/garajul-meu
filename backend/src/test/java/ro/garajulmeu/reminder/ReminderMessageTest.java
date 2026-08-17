package ro.garajulmeu.reminder;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ro.garajulmeu.push.PushNotification;
import ro.garajulmeu.user.Language;
import ro.garajulmeu.vehicledocument.DocumentType;

import static org.assertj.core.api.Assertions.assertThat;

/** No Spring: composing a sentence needs a language and a date, and nothing else. */
class ReminderMessageTest {

	private static final LocalDate EXPIRES = LocalDate.of(2026, 12, 1);

	private static ReminderMessage.Subject subject(DocumentType type, String label) {
		return new ReminderMessage.Subject(UUID.randomUUID(), UUID.randomUUID(), type,
				EXPIRES, label);
	}

	/**
	 * The rule that has nothing to do with software: Romanian needs "de" above
	 * nineteen. Thirty is the only default offset that trips it, which is exactly
	 * why it is asserted next to one that does not.
	 */
	@Test
	void romanianSaysDeZileAboveNineteenAndZileBelow() {
		assertThat(ReminderMessage.title(DocumentType.RCA, 30, Language.RO))
				.isEqualTo("RCA expiră în 30 de zile");
		assertThat(ReminderMessage.title(DocumentType.RCA, 14, Language.RO))
				.isEqualTo("RCA expiră în 14 zile");
		assertThat(ReminderMessage.title(DocumentType.RCA, 3, Language.RO))
				.isEqualTo("RCA expiră în 3 zile");
	}

	/** Nobody says "in 1 days" or "în 0 zile" in either language. */
	@Test
	void oneDayAndTheDayItselfAreWordsRatherThanNumbers() {
		assertThat(ReminderMessage.title(DocumentType.ITP, 1, Language.RO))
				.isEqualTo("ITP expiră mâine");
		assertThat(ReminderMessage.title(DocumentType.ITP, 0, Language.RO))
				.isEqualTo("ITP expiră azi");
		assertThat(ReminderMessage.title(DocumentType.ITP, 1, Language.EN))
				.isEqualTo("ITP expires tomorrow");
		assertThat(ReminderMessage.title(DocumentType.ITP, 0, Language.EN))
				.isEqualTo("ITP expires today");
	}

	/** The same four labels the frontend carries, including the one that translates. */
	@Test
	void onlyRovinietaHasAnEnglishName() {
		assertThat(ReminderMessage.title(DocumentType.ROVINIETA, 7, Language.RO))
				.startsWith("Rovinietă");
		assertThat(ReminderMessage.title(DocumentType.ROVINIETA, 7, Language.EN))
				.startsWith("Road tax");
		assertThat(ReminderMessage.title(DocumentType.CASCO, 7, Language.EN))
				.startsWith("CASCO");
	}

	/** Day-first in both, and a named month in English so 03/12 cannot be misread. */
	@Test
	void theDateIsWrittenTheWayEachLanguageReadsIt() {
		assertThat(ReminderMessage.body("Logan", EXPIRES, Language.RO))
				.isEqualTo("Logan — până la 01.12.2026");
		assertThat(ReminderMessage.body("Logan", EXPIRES, Language.EN))
				.isEqualTo("Logan — until 1 Dec 2026");
	}

	/** A notification that begins with the word "null" is worse than a generic one. */
	@Test
	void aVehicleWithNoLabelIsStillDescribed() {
		assertThat(ReminderMessage.body(null, EXPIRES, Language.RO)).startsWith("Vehiculul tău");
		assertThat(ReminderMessage.body("   ", EXPIRES, Language.EN)).startsWith("Your vehicle");
	}

	/**
	 * Section 18's real constraint: this payload is stored by the operating system
	 * and shown on a locked screen. Three keys, all identifiers, and the test
	 * asserts the whole set rather than the presence of each - so a fourth key
	 * added later has to be argued for here before it reaches a lock screen.
	 */
	@Test
	void thePayloadCarriesIdentifiersAndNothingElse() {
		ReminderMessage.Subject subject = subject(DocumentType.RCA, "B 123 ABC");

		PushNotification notification = ReminderMessage.forSubject(subject, 7, Language.RO);

		assertThat(notification.data()).containsOnlyKeys("vehicleId", "documentId", "type");
		assertThat(notification.data().get("vehicleId")).isEqualTo(subject.vehicleId().toString());
		assertThat(notification.data().get("documentId")).isEqualTo(subject.documentId().toString());
		assertThat(notification.title()).isEqualTo("RCA expiră în 7 zile");
		assertThat(notification.body()).isEqualTo("B 123 ABC — până la 01.12.2026");
	}
}