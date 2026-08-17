package ro.garajulmeu.vehicledocument;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;
import ro.garajulmeu.user.User;
import ro.garajulmeu.user.UserRepository;
import ro.garajulmeu.vehicle.VehicleRepository;
import ro.garajulmeu.vehicledocument.dto.DocumentDetails;
import ro.garajulmeu.vehicledocument.dto.SaveDocumentRequest;

@Service
public class VehicleDocumentService {

	private static final Logger log = LoggerFactory.getLogger(VehicleDocumentService.class);

	private final VehicleDocumentRepository documentRepository;
	private final VehicleRepository vehicleRepository;
	private final UserRepository userRepository;
	private final Clock clock;

	VehicleDocumentService(VehicleDocumentRepository documentRepository,
			VehicleRepository vehicleRepository, UserRepository userRepository, Clock clock) {
		this.documentRepository = documentRepository;
		this.vehicleRepository = vehicleRepository;
		this.userRepository = userRepository;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public List<DocumentDetails> documentsOf(UUID accountId, UUID vehicleId) {
		requireVehicle(accountId, vehicleId);
		LocalDate today = todayFor(accountId);

		return documentRepository.ofVehicle(vehicleId, accountId).stream()
				.map(document -> view(document, today))
				.toList();
	}

	@Transactional(readOnly = true)
	public DocumentDetails detailsOf(UUID accountId, UUID vehicleId, UUID documentId) {
		VehicleDocument document = require(accountId, vehicleId, documentId);
		return view(document, todayFor(accountId));
	}

	@Transactional
	public DocumentDetails add(UUID accountId, UUID vehicleId, SaveDocumentRequest request) {
		requireVehicle(accountId, vehicleId);

		VehicleDocument document = new VehicleDocument(vehicleId, typeOf(request), request.validUntil());
		apply(request, document);
		documentRepository.saveAndFlush(document);

		// The identifier and the type, never the reference number: section 27 keeps
		// a policy number out of the logs as it keeps a verification code out.
		log.info("Added {} document {} to vehicle {}", document.getType(), document.getId(), vehicleId);

		return view(document, todayFor(accountId));
	}

	@Transactional
	public DocumentDetails correct(UUID accountId, UUID vehicleId, UUID documentId,
			SaveDocumentRequest request) {
		VehicleDocument document = require(accountId, vehicleId, documentId);

		document.setType(typeOf(request));
		document.setValidUntil(request.validUntil());
		apply(request, document);
		documentRepository.saveAndFlush(document);

		return view(document, todayFor(accountId));
	}

	@Transactional
	public void delete(UUID accountId, UUID vehicleId, UUID documentId) {
		VehicleDocument document = require(accountId, vehicleId, documentId);

		documentRepository.delete(document);
		documentRepository.flush();
		log.info("Deleted document {} of vehicle {}", documentId, vehicleId);
	}

	/**
	 * Everything a correction replaces wholesale, and the one rule section 12
	 * states about the pair of dates. The database holds the same rule in a CHECK
	 * constraint - this is the version that produces a named error rather than a
	 * violation, and the constraint is what makes the rule true of the data
	 * however a row arrives.
	 */
	private static void apply(SaveDocumentRequest request, VehicleDocument document) {
		if (request.validFrom() != null && request.validFrom().isAfter(request.validUntil())) {
			throw new ApiException(ErrorCode.DOCUMENT_INVALID_DATE_RANGE);
		}

		document.setValidFrom(request.validFrom());
		document.setProvider(trimmedOrNull(request.provider()));
		document.setReferenceNumber(trimmedOrNull(request.referenceNumber()));
		document.setNotes(trimmedOrNull(request.notes()));
	}

	private static DocumentType typeOf(SaveDocumentRequest request) {
		return DocumentType.of(request.type())
				.orElseThrow(() -> new ApiException(ErrorCode.DOCUMENT_TYPE_INVALID));
	}

	private static String trimmedOrNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static DocumentDetails view(VehicleDocument document, LocalDate today) {
		return new DocumentDetails(
				document.getId(),
				document.getType(),
				document.getValidFrom(),
				document.getValidUntil(),
				document.getProvider(),
				document.getReferenceNumber(),
				document.getNotes(),
				DocumentCoverage.statusOn(today, document.getValidUntil()),
				DocumentCoverage.daysRemaining(today, document.getValidUntil()));
	}

	/**
	 * Asked for even when the answer is not used, so that documents of a vehicle
	 * the caller does not own answer 404 rather than an empty list. An empty list
	 * would be indistinguishable from a vehicle that simply has no documents,
	 * which is a different fact and would read as one.
	 */
	private void requireVehicle(UUID accountId, UUID vehicleId) {
		vehicleRepository.findByIdAndUserId(vehicleId, accountId)
				.orElseThrow(() -> new ApiException(ErrorCode.VEHICLE_NOT_FOUND));
	}

	private VehicleDocument require(UUID accountId, UUID vehicleId, UUID documentId) {
		return documentRepository.byIdOfVehicle(documentId, vehicleId, accountId)
				.orElseThrow(() -> new ApiException(ErrorCode.DOCUMENT_NOT_FOUND));
	}

	/**
	 * The reader's today. Section 11 counts remaining days in the user's IANA
	 * timezone, so the account is loaded for one column - a cost paid knowingly,
	 * because the alternative is storing a status that goes stale at midnight.
	 * The zone is not re-validated here: the profile endpoint refuses one that
	 * does not exist, and guarding again would be defending against a state the
	 * application prevents.
	 */
	private LocalDate todayFor(UUID accountId) {
		User user = userRepository.findById(accountId)
				.orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

		return DocumentCoverage.todayFor(clock, ZoneId.of(user.getTimezone()));
	}
}