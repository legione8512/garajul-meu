package ro.garajulmeu.ocr;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import ro.garajulmeu.TestcontainersConfiguration;
import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;
import ro.garajulmeu.user.User;
import ro.garajulmeu.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Three annotations and no MockMvc, matching RefreshTokenServiceTest exactly, so
 * the cached context is reused and no further container starts.
 *
 * <p>The clock is never moved. Every case is set up by writing rows for the days
 * it needs, which is both closer to what the database will really hold and free
 * of a mutable Clock bean - one of those would change the context cache key and
 * start a fifth container for the sake of a date.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class OcrQuotaTest {

	@Autowired
	private OcrQuota quota;

	@Autowired
	private OcrUsageRepository usageRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private OcrProperties properties;

	private UUID givenAccount(String email) {
		return userRepository.saveAndFlush(new User("Marius Robert", email, "argon2-placeholder")).getId();
	}

	private void givenSpent(UUID accountId, LocalDate day, int requests) {
		OcrUsage usage = new OcrUsage(accountId, day);
		for (int i = 0; i < requests; i++) {
			usage.increment();
		}
		usageRepository.saveAndFlush(usage);
	}

	private LocalDate today() {
		return LocalDate.now();
	}

	@Test
	void theFirstRequestOfTheDayIsAllowedAndCounted() {
		UUID account = givenAccount("first@example.com");

		quota.consume(account);

		assertThat(usageRepository.findByUserIdAndUsageDate(account, today()))
				.get()
				.extracting(OcrUsage::getRequestCount)
				.isEqualTo(1);
	}

	@Test
	void theDailyAllowanceRunsOut() {
		UUID account = givenAccount("daily@example.com");
		givenSpent(account, today(), properties.dailyLimit());

		assertThatThrownBy(() -> quota.consume(account))
				.isInstanceOf(ApiException.class)
				.extracting(thrown -> ((ApiException) thrown).errorCode())
				.isEqualTo(ErrorCode.OCR_RATE_LIMITED);
	}

	/** A refused caller has not spent anything, so tomorrow is not shorter. */
	@Test
	void beingRefusedCostsNothing() {
		UUID account = givenAccount("refused@example.com");
		givenSpent(account, today(), properties.dailyLimit());

		assertThatThrownBy(() -> quota.consume(account)).isInstanceOf(ApiException.class);

		assertThat(usageRepository.findByUserIdAndUsageDate(account, today()))
				.get()
				.extracting(OcrUsage::getRequestCount)
				.isEqualTo(properties.dailyLimit());
	}

	/** Yesterday's spending does not follow you into today. */
	@Test
	void aNewDayBringsAFreshDailyAllowance() {
		UUID account = givenAccount("newday@example.com");
		givenSpent(account, today().minusDays(1), properties.dailyLimit());

		quota.consume(account);

		assertThat(usageRepository.findByUserIdAndUsageDate(account, today()))
				.get()
				.extracting(OcrUsage::getRequestCount)
				.isEqualTo(1);
	}

	/**
	 * The month is the sum of its days, and it can run out while the day is still
	 * untouched - which is the whole reason the monthly figure is not simply the
	 * daily one multiplied.
	 */
	@Test
	void theMonthlyAllowanceRunsOutEvenOnAnEmptyDay() {
		UUID account = givenAccount("monthly@example.com");
		LocalDate firstOfMonth = today().withDayOfMonth(1);

		int spent = 0;
		for (int day = 0; spent < properties.monthlyLimit(); day++) {
			LocalDate on = firstOfMonth.plusDays(day);
			if (on.isEqual(today()) || on.isAfter(today())) {
				break;
			}
			int requests = Math.min(properties.dailyLimit(), properties.monthlyLimit() - spent);
			givenSpent(account, on, requests);
			spent += requests;
		}

		// Only meaningful once enough earlier days exist to reach the monthly
		// limit; on the first days of a month they do not, and the daily limit is
		// the one that binds.
		if (spent >= properties.monthlyLimit()) {
			assertThatThrownBy(() -> quota.consume(account))
					.isInstanceOf(ApiException.class)
					.extracting(thrown -> ((ApiException) thrown).errorCode())
					.isEqualTo(ErrorCode.OCR_RATE_LIMITED);
		} else {
			quota.consume(account);
		}
	}

	/** Section 24: deletion covers everything the account owns, this included. */
	@Test
	void deletingTheAccountTakesItsUsageWithIt() {
		UUID account = givenAccount("deleted@example.com");
		quota.consume(account);
		assertThat(usageRepository.count()).isEqualTo(1);

		userRepository.deleteById(account);
		userRepository.flush();

		assertThat(usageRepository.count()).isZero();
	}

	@Test
	void twoAccountsDoNotShareAnAllowance() {
		UUID one = givenAccount("one@example.com");
		UUID two = givenAccount("two@example.com");
		givenSpent(one, today(), properties.dailyLimit());

		quota.consume(two);

		assertThat(usageRepository.findByUserIdAndUsageDate(two, today()))
				.get()
				.extracting(OcrUsage::getRequestCount)
				.isEqualTo(1);
	}
}