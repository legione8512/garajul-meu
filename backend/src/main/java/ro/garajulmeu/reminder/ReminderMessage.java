package ro.garajulmeu.reminder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import ro.garajulmeu.push.PushNotification;
import ro.garajulmeu.user.Language;
import ro.garajulmeu.vehicledocument.DocumentType;
import ro.garajulmeu.vehiclepayment.PaymentKind;

/**
 * The words that reach a lock screen, in the reader's language. Specification
 * section 18.
 *
 * <p><strong>The backend composes user-facing text here, and only here.</strong>
 * Section 17's rule that the backend never sends wording is about API errors,
 * where a frontend is running and can translate a code. A push notification
 * arrives when the application is not running at all, so nobody else can write
 * it. That makes this the one place where a translation lives on this side of
 * the wire - and the duplication with the frontend's locale files is real,
 * unavoidable, and worth knowing about.
 *
 * <p><strong>What is deliberately absent:</strong> the VIN, the owner's address
 * and the policy number, which section 18 names. The vehicle's label is included
 * because a garage with three cars gets nothing from "a document expires", and
 * because a registration plate is displayed on the outside of the car by law -
 * which is not true of any of the three the specification lists.
 */
public final class ReminderMessage {

	private static final DateTimeFormatter ROMANIAN = DateTimeFormatter.ofPattern("dd.MM.yyyy");

	/** Day-first with a named month, so neither reader can misread 03/12. */
	private static final DateTimeFormatter ENGLISH =
			DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

	private ReminderMessage() {
	}

	/**
	 * Everything a notification needs to know about one document, and nothing
	 * else. A record rather than the entity because a {@code VehicleDocument} that
	 * has not been saved has no id yet, which would make this class untestable
	 * without a database - and because 11.4b's query can project straight into it.
	 *
	 * @param vehicleLabel what the owner calls the car, or its registration
	 *                     number. Never the VIN.
	 */
	public record Subject(UUID vehicleId, UUID documentId, DocumentType type,
			LocalDate validUntil, String vehicleLabel) {
	}

	public static PushNotification forSubject(Subject subject, int offsetDays, Language language) {
		return new PushNotification(
				title(subject.type(), offsetDays, language),
				body(subject.vehicleLabel(), subject.validUntil(), language),
				// Identifiers only: section 18 asks the notification to deep link to
				// the vehicle and document detail, and that needs exactly these.
				Map.of("vehicleId", subject.vehicleId().toString(),
						"documentId", subject.documentId().toString(),
						"type", subject.type().name()));
	}

	/**
	 * One instalment of a recurring payment, 1.1. No amount, because none is
	 * stored; the lock screen says which car and which day, and nothing about
	 * the contract.
	 */
	public record Instalment(UUID vehicleId, UUID paymentId, PaymentKind kind, LocalDate dueDate,
			String vehicleLabel) {
	}

	public static PushNotification forInstalment(Instalment instalment, int offsetDays,
			Language language) {
		return new PushNotification(
				instalmentTitle(instalment.kind(), offsetDays, language),
				instalmentBody(instalment.vehicleLabel(), instalment.dueDate(), language),
				Map.of("vehicleId", instalment.vehicleId().toString(),
						"paymentId", instalment.paymentId().toString(),
						"kind", instalment.kind().name()));
	}

	static String instalmentTitle(PaymentKind kind, int offsetDays, Language language) {
		String name = instalmentLabel(kind, language);

		return switch (language) {
			case RO -> switch (offsetDays) {
				case 0 -> name + " e azi";
				case 1 -> name + " e mâine";
				default -> name + " e peste " + offsetDays + (needsDe(offsetDays) ? " de zile" : " zile");
			};
			case EN -> switch (offsetDays) {
				case 0 -> name + " is due today";
				case 1 -> name + " is due tomorrow";
				default -> name + " is due in " + offsetDays + " days";
			};
		};
	}

	static String instalmentBody(String vehicleLabel, LocalDate dueDate, Language language) {
		return switch (language) {
			case RO -> named(vehicleLabel, "Vehiculul tău") + " — scadentă pe " + ROMANIAN.format(dueDate);
			case EN -> named(vehicleLabel, "Your vehicle") + " — due on " + ENGLISH.format(dueDate);
		};
	}

	/** The same names the frontend's locale files give the two kinds. */
	private static String instalmentLabel(PaymentKind kind, Language language) {
		return switch (kind) {
			case LOAN -> language == Language.RO ? "Rata la leasing/credit" : "Loan instalment";
			case CASCO -> language == Language.RO ? "Rata CASCO" : "CASCO instalment";
		};
	}

	static String title(DocumentType type, int offsetDays, Language language) {
		String name = label(type, language);

		return switch (language) {
			case RO -> switch (offsetDays) {
				case 0 -> name + " expiră azi";
				case 1 -> name + " expiră mâine";
				default -> name + " expiră în " + offsetDays + (needsDe(offsetDays) ? " de zile" : " zile");
			};
			case EN -> switch (offsetDays) {
				case 0 -> name + " expires today";
				case 1 -> name + " expires tomorrow";
				default -> name + " expires in " + offsetDays + " days";
			};
		};
	}

	static String body(String vehicleLabel, LocalDate validUntil, Language language) {
		return switch (language) {
			case RO -> named(vehicleLabel, "Vehiculul tău") + " — până la " + ROMANIAN.format(validUntil);
			case EN -> named(vehicleLabel, "Your vehicle") + " — until " + ENGLISH.format(validUntil);
		};
	}

	/**
	 * Romanian counts differently above nineteen: "în 3 zile", but "în 30 de
	 * zile". Written as the language's rule rather than as a special case for the
	 * one offset that needs it today, so that adding a sixty-day offset later does
	 * not quietly produce "în 60 zile" - which reads as a mistake to every
	 * Romanian speaker and to no test.
	 */
	private static boolean needsDe(int count) {
		int lastTwo = count % 100;
		return lastTwo == 0 || lastTwo >= 20;
	}

	/**
	 * A vehicle always has a registration number, so the fallback should never
	 * fire. It exists because the alternative to a generic word is a notification
	 * that begins with the word "null".
	 */
	private static String named(String vehicleLabel, String fallback) {
		return vehicleLabel == null || vehicleLabel.isBlank() ? fallback : vehicleLabel;
	}

	/**
	 * The same labels the frontend's locale files carry, and the same choices:
	 * RCA, CASCO and ITP stay as they are in English too, because they are the
	 * names Romanian drivers use and translating them would help nobody; the
	 * road tax and the equipment added in 1.0.2 are ordinary words, and read in
	 * the reader's language.
	 */
	private static String label(DocumentType type, Language language) {
		return switch (type) {
			case RCA -> "RCA";
			case CASCO -> "CASCO";
			case ITP -> "ITP";
			case ROVINIETA -> language == Language.RO ? "Rovinietă" : "Road tax";
			case EXTINGUISHER -> language == Language.RO ? "Extinctor" : "Fire extinguisher";
			case FIRST_AID_KIT -> language == Language.RO ? "Trusă medicală" : "First aid kit";
		};
	}
}