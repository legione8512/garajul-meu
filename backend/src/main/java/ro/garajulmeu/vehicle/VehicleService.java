package ro.garajulmeu.vehicle;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;
import ro.garajulmeu.registrationcertificate.CertificateValues;
import ro.garajulmeu.registrationcertificate.RegistrationCertificate;
import ro.garajulmeu.registrationcertificate.RegistrationCertificateRepository;
import ro.garajulmeu.vehicle.dto.CreateVehicleRequest;
import ro.garajulmeu.vehicle.dto.UpdateVehicleRequest;
import ro.garajulmeu.vehicle.dto.VehicleDetails;
import ro.garajulmeu.vehicle.dto.VehicleSummary;

@Service
public class VehicleService {

	private static final Logger log = LoggerFactory.getLogger(VehicleService.class);

	private final VehicleRepository vehicleRepository;
	private final RegistrationCertificateRepository certificateRepository;

	VehicleService(VehicleRepository vehicleRepository,
			RegistrationCertificateRepository certificateRepository) {
		this.vehicleRepository = vehicleRepository;
		this.certificateRepository = certificateRepository;
	}

	@Transactional(readOnly = true)
	public List<VehicleSummary> garageOf(UUID accountId) {
		return vehicleRepository.summariesOf(accountId);
	}

	/**
	 * A vehicle that belongs to somebody else answers exactly as one that does
	 * not exist. Answering VEHICLE_ACCESS_DENIED instead would confirm that the
	 * identifier is real, which turns the endpoint into a way of discovering
	 * other people's vehicles one guess at a time. That code stays in the
	 * catalogue for a V1 that has no shared ownership to use it.
	 */
	@Transactional(readOnly = true)
	public VehicleDetails detailsOf(UUID accountId, UUID vehicleId) {
		return vehicleRepository.detailsOf(vehicleId, accountId)
				.orElseThrow(() -> new ApiException(ErrorCode.VEHICLE_NOT_FOUND));
	}

	/**
	 * The vehicle and its certificate are written together or not at all. Section
	 * 9 leaves a vehicle with no identity of its own, so a vehicle whose
	 * certificate insert failed would be a row nobody could name and nobody could
	 * find - the transaction is what prevents that, and it is the reason the
	 * contract specifies this endpoint as atomic.
	 *
	 * <p>Duplicate VINs are refused twice over. The {@code exists} check gives the
	 * ordinary case a clean answer; the unique index catches two simultaneous
	 * requests that both passed it, which is the same arrangement registration
	 * uses around {@code ux_users_email}. The check alone would be a race, and the
	 * index alone would report a real duplicate as an internal error.
	 */
	@Transactional
	public VehicleDetails create(UUID accountId, CreateVehicleRequest request) {
		String vin = CertificateValues.normalisedVin(request.vin());

		if (certificateRepository.existsByUserIdAndVin(accountId, vin)) {
			throw new ApiException(ErrorCode.VEHICLE_DUPLICATE_VIN);
		}

		Vehicle vehicle = new Vehicle(accountId);
		vehicle.setDisplayName(CertificateValues.trimmedOrNull(request.displayName()));

		// Flushed rather than merely saved. The two entities are not linked by a
		// mapped association, so Hibernate has nothing telling it which insert
		// depends on the other and is free to order them by entity type - which
		// would put the certificate ahead of the vehicle it points at and break
		// the foreign key. Writing the vehicle now removes the question.
		vehicleRepository.saveAndFlush(vehicle);

		RegistrationCertificate certificate = new RegistrationCertificate(
				vehicle.getId(),
				accountId,
				CertificateValues.normalisedRegistrationNumber(request.registrationNumber()),
				request.make().trim(),
				request.commercialDescription().trim(),
				vin);

		try {
			certificateRepository.saveAndFlush(certificate);
		} catch (DataIntegrityViolationException exception) {
			throw new ApiException(ErrorCode.VEHICLE_DUPLICATE_VIN);
		}

		log.info("Created vehicle {} for account {}", vehicle.getId(), accountId);

		return new VehicleDetails(vehicle.getId(), vehicle.getDisplayName(),
				certificate.getRegistrationNumber(), certificate.getMake(),
				certificate.getCommercialDescription(), certificate.getVin(),
				vehicle.getCreatedAt());
	}

	@Transactional
	public VehicleDetails rename(UUID accountId, UUID vehicleId, UpdateVehicleRequest request) {
		Vehicle vehicle = vehicleRepository.findByIdAndUserId(vehicleId, accountId)
				.orElseThrow(() -> new ApiException(ErrorCode.VEHICLE_NOT_FOUND));

		if (request.displayName() != null) {
			vehicle.setDisplayName(CertificateValues.trimmedOrNull(request.displayName()));
		}
		vehicleRepository.saveAndFlush(vehicle);

		return vehicleRepository.detailsOf(vehicleId, accountId)
				.orElseThrow(() -> new ApiException(ErrorCode.VEHICLE_NOT_FOUND));
	}

	/**
	 * The certificate goes with it, removed by the foreign key rather than by
	 * anything here - the same arrangement that carries verification tokens and
	 * refresh tokens away with a deleted account, and for the same reason: there
	 * is no mapped association for Hibernate to cascade along.
	 */
	@Transactional
	public void delete(UUID accountId, UUID vehicleId) {
		Vehicle vehicle = vehicleRepository.findByIdAndUserId(vehicleId, accountId)
				.orElseThrow(() -> new ApiException(ErrorCode.VEHICLE_NOT_FOUND));

		vehicleRepository.delete(vehicle);
		vehicleRepository.flush();
		log.info("Deleted vehicle {} of account {}", vehicleId, accountId);
	}
}