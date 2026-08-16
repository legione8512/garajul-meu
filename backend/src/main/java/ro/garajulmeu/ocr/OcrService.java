package ro.garajulmeu.ocr;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * One scan, in the order the order matters.
 *
 * <p><strong>The upload is checked before any allowance is spent.</strong>
 * Somebody who sent a broken file asked nothing of the provider and should be
 * charged for nothing; section 13's ten a day are requests that cost money, not
 * attempts to make one.
 *
 * <p><strong>This method is deliberately not transactional.</strong>
 * {@link OcrQuota#consume} holds a pessimistic lock on the day's row, and it has
 * to be released before the provider is called - otherwise a second request from
 * the same account would wait for the length of an OCR round trip, holding a
 * pooled connection while it did. Each step owns its own transaction, and the
 * external call happens outside all of them.
 *
 * <p>Nothing is saved. Section 16 describes the endpoint as "Multipart OCR; no
 * vehicle save", and section 13 requires the person to review and correct before
 * anything is stored - so the answer is a proposal and the existing PATCH on the
 * certificate remains the only way in.
 */
@Service
public class OcrService {

	private static final Logger log = LoggerFactory.getLogger(OcrService.class);

	private final OcrImageValidator validator;
	private final OcrQuota quota;
	private final OcrProvider provider;
	private final RomanianRegistrationCertificateMapper mapper;

	OcrService(OcrImageValidator validator, OcrQuota quota, OcrProvider provider,
			RomanianRegistrationCertificateMapper mapper) {
		this.validator = validator;
		this.quota = quota;
		this.provider = provider;
		this.mapper = mapper;
	}

	public OcrScan scan(UUID accountId, byte[] upload) {
		OcrImage image = validator.accept(upload);

		quota.consume(accountId);

		OcrDocument document = provider.read(image);
		OcrScan scan = mapper.map(document);

		// The count, never the contents. Section 24 forbids logging certificate
		// images and unnecessary OCR payloads, and the values read off somebody's
		// certificate are exactly that.
		log.info("Scanned a certificate for account {}: {} fields proposed",
				accountId, scan.fields().size());

		return scan;
	}
}