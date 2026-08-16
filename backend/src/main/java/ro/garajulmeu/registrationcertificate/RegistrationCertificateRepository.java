package ro.garajulmeu.registrationcertificate;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationCertificateRepository extends JpaRepository<RegistrationCertificate, UUID> {

	/**
	 * The section 9 rule, asked before inserting so the normal duplicate gets a
	 * clean answer instead of a constraint violation. It is not the guarantee -
	 * two simultaneous requests can both pass it - which is what
	 * ux_registration_certificates_user_vin is for.
	 */
	boolean existsByUserIdAndVin(UUID userId, String vin);

	/**
	 * The account is part of the query, as everywhere in the vehicle domain: a
	 * certificate belonging to somebody else is not found rather than refused.
	 */
	Optional<RegistrationCertificate> findByVehicleIdAndUserId(UUID vehicleId, UUID userId);
}