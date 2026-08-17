package ro.garajulmeu.dashboard;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;
import ro.garajulmeu.dashboard.dto.DashboardVehicle;
import ro.garajulmeu.dashboard.dto.DashboardView;
import ro.garajulmeu.dashboard.dto.DocumentStatusLine;
import ro.garajulmeu.user.User;
import ro.garajulmeu.user.UserRepository;
import ro.garajulmeu.vehicle.VehicleRepository;
import ro.garajulmeu.vehicle.dto.VehicleSummary;
import ro.garajulmeu.vehicledocument.DocumentCoverage;
import ro.garajulmeu.vehicledocument.DocumentStatus;
import ro.garajulmeu.vehicledocument.DocumentType;
import ro.garajulmeu.vehicledocument.VehicleDocument;
import ro.garajulmeu.vehicledocument.VehicleDocumentRepository;

/**
 * The projection section 11 describes and section 16 names.
 *
 * <p><strong>Three queries, whatever the garage holds</strong>: the vehicles,
 * every document of every one of them, and the account's timezone. The grouping
 * by vehicle and by type happens over rows already in hand, because asking the
 * database per vehicle per type would be four round trips for each car on a
 * screen whose whole job is to load at once.
 */
@Service
public class DashboardService {

	private final VehicleRepository vehicleRepository;
	private final VehicleDocumentRepository documentRepository;
	private final UserRepository userRepository;
	private final Clock clock;

	DashboardService(VehicleRepository vehicleRepository,
			VehicleDocumentRepository documentRepository, UserRepository userRepository, Clock clock) {
		this.vehicleRepository = vehicleRepository;
		this.documentRepository = documentRepository;
		this.userRepository = userRepository;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public DashboardView of(UUID accountId) {
		LocalDate today = todayFor(accountId);

		Map<UUID, List<VehicleDocument>> byVehicle = documentRepository.ofGarage(accountId).stream()
				.collect(Collectors.groupingBy(VehicleDocument::getVehicleId));

		List<DashboardVehicle> vehicles = new ArrayList<>();

		for (VehicleSummary summary : vehicleRepository.summariesOf(accountId)) {
			List<VehicleDocument> documents = byVehicle.getOrDefault(summary.id(), List.of());

			vehicles.add(new DashboardVehicle(
					summary.id(),
					summary.displayName(),
					summary.registrationNumber(),
					summary.make(),
					summary.commercialDescription(),
					lines(documents, today)));
		}

		return new DashboardView(List.copyOf(vehicles));
	}

	/** Every type, in the order the enum declares them, whether configured or not. */
	private static List<DocumentStatusLine> lines(List<VehicleDocument> documents, LocalDate today) {
		Map<DocumentType, List<VehicleDocument>> byType = documents.stream()
				.collect(Collectors.groupingBy(VehicleDocument::getType));

		List<DocumentStatusLine> lines = new ArrayList<>();

		for (DocumentType type : DocumentType.values()) {
			lines.add(line(type, byType.getOrDefault(type, List.of()), today));
		}

		return List.copyOf(lines);
	}

	/**
	 * Section 11's three cases, in the order it states them.
	 *
	 * <p>A record covers today: its status is the answer, and nothing else needs
	 * saying. Nothing has ever been entered: NOT_CONFIGURED, the one state the
	 * specification calls presentation-only. Records exist but none covers today:
	 * <strong>the gap is the answer, never the future record</strong> - section 11
	 * is explicit that an upcoming policy must not be shown as active - so the
	 * status is EXPIRED, {@code validUntil} says when cover actually lapsed if it
	 * ever began, and {@code upcomingFrom} says when it resumes if it will.
	 */
	private static DocumentStatusLine line(DocumentType type, List<VehicleDocument> ofType,
			LocalDate today) {
		Optional<VehicleDocument> covering = DocumentCoverage.coveringOn(ofType, today);

		if (covering.isPresent()) {
			VehicleDocument document = covering.get();
			return new DocumentStatusLine(type,
					DocumentCoverage.statusOn(today, document.getValidUntil()),
					document.getId(),
					document.getValidUntil(),
					DocumentCoverage.daysRemaining(today, document.getValidUntil()),
					null);
		}

		if (ofType.isEmpty()) {
			return new DocumentStatusLine(type, DocumentStatus.NOT_CONFIGURED, null, null, null, null);
		}

		Optional<VehicleDocument> lapsed = DocumentCoverage.lastToExpire(ofType, today);
		LocalDate resumes = DocumentCoverage.upcomingAfter(ofType, today)
				.map(VehicleDocument::getValidFrom)
				.orElse(null);

		return new DocumentStatusLine(type,
				DocumentStatus.EXPIRED,
				lapsed.map(VehicleDocument::getId).orElse(null),
				lapsed.map(VehicleDocument::getValidUntil).orElse(null),
				lapsed.map(document -> DocumentCoverage.daysRemaining(today, document.getValidUntil()))
						.orElse(null),
				resumes);
	}

	/**
	 * The reader's today, as the document endpoints compute it. The account is
	 * loaded for one column, knowingly: the alternative is storing a status that
	 * goes stale at midnight.
	 */
	private LocalDate todayFor(UUID accountId) {
		User user = userRepository.findById(accountId)
				.orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

		return DocumentCoverage.todayFor(clock, ZoneId.of(user.getTimezone()));
	}
}