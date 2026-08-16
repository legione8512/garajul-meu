package ro.garajulmeu.registrationcertificate;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;
import ro.garajulmeu.registrationcertificate.dto.CertificateData;

@Service
public class RegistrationCertificateService {

	private static final Logger log = LoggerFactory.getLogger(RegistrationCertificateService.class);

	private final RegistrationCertificateRepository certificateRepository;

	RegistrationCertificateService(RegistrationCertificateRepository certificateRepository) {
		this.certificateRepository = certificateRepository;
	}

	@Transactional(readOnly = true)
	public CertificateData of(UUID accountId, UUID vehicleId) {
		return CertificateData.of(load(accountId, vehicleId));
	}

	/**
	 * Correcting the certificate can change the VIN, so it goes through the same
	 * normalisation and the same uniqueness rule as adding a vehicle. Without
	 * that, section 9 would hold at the front door and not afterwards: you could
	 * correct one vehicle's VIN into another's.
	 *
	 * <p>The check is skipped when the VIN has not changed - a certificate is
	 * otherwise a duplicate of itself, and every correction of a colour would be
	 * refused.
	 */
	@Transactional
	public CertificateData correct(UUID accountId, UUID vehicleId, CertificateData data) {
		RegistrationCertificate certificate = load(accountId, vehicleId);

		String vin = CertificateValues.normalisedVin(data.vin());
		if (!vin.equals(certificate.getVin()) && certificateRepository.existsByUserIdAndVin(accountId, vin)) {
			throw new ApiException(ErrorCode.VEHICLE_DUPLICATE_VIN);
		}

		data.applyTo(certificate);

		try {
			certificateRepository.saveAndFlush(certificate);
		} catch (DataIntegrityViolationException exception) {
			throw new ApiException(ErrorCode.VEHICLE_DUPLICATE_VIN);
		}

		// Section 8 forbids logging C.2 and C.3, and that ban covers naming which
		// fields changed as much as their values: "owner_address" in a log line is
		// itself a statement about somebody. So this says that a correction
		// happened and to which vehicle, and nothing about what is in it.
		log.info("Corrected the registration certificate of vehicle {}", vehicleId);

		return CertificateData.of(certificate);
	}

	/**
	 * A certificate is one-to-one with its vehicle and cascades with it, so a
	 * missing one means the vehicle is not yours or does not exist - answered as
	 * VEHICLE_NOT_FOUND, the same 404 the vehicle endpoints give, so this route
	 * cannot become a way to discover which identifiers are real.
	 * REGISTRATION_CERTIFICATE_NOT_FOUND stays in the catalogue unused: in V1
	 * nothing can produce a vehicle without one.
	 */
	private RegistrationCertificate load(UUID accountId, UUID vehicleId) {
		return certificateRepository.findByVehicleIdAndUserId(vehicleId, accountId)
				.orElseThrow(() -> new ApiException(ErrorCode.VEHICLE_NOT_FOUND));
	}
}