package ro.garajulmeu.ocr;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;

/**
 * The OCR allowance from specification section 13, spent one request at a time.
 *
 * <p>Counted in the database rather than in memory, unlike the authentication
 * rate limits. Those exist to make guessing expensive, and forgiving them on a
 * restart costs nothing; this one exists because each request costs money at a
 * provider, so a restart must not hand every account a fresh budget.
 *
 * <p><strong>Attempts are counted, not successes.</strong> The provider is paid
 * for the call, not for the answer, so a refund on failure would be a discount
 * we do not receive. It also removes the shape where a failing provider lets one
 * account spend without limit.
 */
@Service
public class OcrQuota {

	private static final Logger log = LoggerFactory.getLogger(OcrQuota.class);

	private final OcrUsageRepository usageRepository;
	private final OcrProperties properties;
	private final Clock clock;

	OcrQuota(OcrUsageRepository usageRepository, OcrProperties properties, Clock clock) {
		this.usageRepository = usageRepository;
		this.properties = properties;
		this.clock = clock;
	}

	/**
	 * Takes one request from the allowance, or refuses.
	 *
	 * <p>The row is created if it is missing, then locked, then read - so the
	 * check and the increment cannot be split by another request arriving at the
	 * same moment. Nothing is incremented when the answer is no: a refused caller
	 * has not spent anything.
	 */
	@Transactional
	public void consume(UUID accountId) {
		LocalDate today = LocalDate.now(clock);

		usageRepository.ensureRowFor(accountId, today);

		OcrUsage usage = usageRepository.findByUserIdAndUsageDate(accountId, today).orElseThrow();
		long thisMonth = usageRepository.totalBetween(accountId, today.withDayOfMonth(1), today);

		if (usage.getRequestCount() >= properties.dailyLimit() || thisMonth >= properties.monthlyLimit()) {
			log.info("OCR allowance exhausted for account {}", accountId);
			throw new ApiException(ErrorCode.OCR_RATE_LIMITED);
		}

		usage.increment();
		usageRepository.saveAndFlush(usage);
	}
}