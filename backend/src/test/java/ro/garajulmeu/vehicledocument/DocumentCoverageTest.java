package ro.garajulmeu.vehicledocument;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** No Spring, no database: section 11's rules are arithmetic on two dates. */
class DocumentCoverageTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 8, 17);

	private static DocumentStatus statusIn(long days) {
		return DocumentCoverage.statusOn(TODAY, TODAY.plusDays(days));
	}

	/**
	 * Every boundary in section 11's table, and only the boundaries - a status
	 * asserted in the middle of a band proves nothing an off-by-one would break.
	 */
	@Test
	void theBandsMeetExactlyWhereSectionElevenSaysTheyDo() {
		assertThat(statusIn(31)).isEqualTo(DocumentStatus.ACTIVE);
		assertThat(statusIn(30)).isEqualTo(DocumentStatus.EXPIRING_SOON);
		assertThat(statusIn(8)).isEqualTo(DocumentStatus.EXPIRING_SOON);
		assertThat(statusIn(7)).isEqualTo(DocumentStatus.URGENT);
		assertThat(statusIn(1)).isEqualTo(DocumentStatus.URGENT);
		assertThat(statusIn(0)).isEqualTo(DocumentStatus.EXPIRES_TODAY);
		assertThat(statusIn(-1)).isEqualTo(DocumentStatus.EXPIRED);
	}

	/**
	 * The sentence in section 11 that costs a day if it is read carelessly: an
	 * insurance policy that ends today still covers you today.
	 */
	@Test
	void aDocumentIsNotExpiredOnTheDayItExpires() {
		assertThat(DocumentCoverage.statusOn(TODAY, TODAY)).isNotEqualTo(DocumentStatus.EXPIRED);
		assertThat(DocumentCoverage.daysRemaining(TODAY, TODAY)).isZero();
		assertThat(DocumentCoverage.statusOn(TODAY.plusDays(1), TODAY)).isEqualTo(DocumentStatus.EXPIRED);
	}

	/**
	 * Section 11 counts in the user's timezone rather than against UTC, so at this
	 * instant it is already tomorrow in Bucharest and still today in London - and
	 * the same row reads differently to the two of them. Written as a test because
	 * the identical rule, left implicit in OcrQuota, disagreed with itself between
	 * midnight and three in the morning.
	 */
	@Test
	void theDayBelongsToTheReaderRatherThanToTheServer() {
		Clock clock = Clock.fixed(Instant.parse("2026-08-17T22:30:00Z"), ZoneId.of("UTC"));

		LocalDate inBucharest = DocumentCoverage.todayFor(clock, ZoneId.of("Europe/Bucharest"));
		LocalDate inLondon = DocumentCoverage.todayFor(clock, ZoneId.of("Europe/London"));

		assertThat(inBucharest).isEqualTo(LocalDate.of(2026, 8, 18));
		assertThat(inLondon).isEqualTo(LocalDate.of(2026, 8, 17));

		LocalDate expiresOn = LocalDate.of(2026, 8, 17);
		assertThat(DocumentCoverage.statusOn(inBucharest, expiresOn)).isEqualTo(DocumentStatus.EXPIRED);
		assertThat(DocumentCoverage.statusOn(inLondon, expiresOn)).isEqualTo(DocumentStatus.EXPIRES_TODAY);
	}
}