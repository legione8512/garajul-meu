package ro.garajulmeu.vehicle;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ro.garajulmeu.vehicle.dto.VehicleDetails;
import ro.garajulmeu.vehicle.dto.VehicleSummary;

/**
 * <p><strong>Ownership is part of every query, not a check that follows one.</strong>
 * Section 15 says knowing a UUID must never be enough to reach another account's
 * data, and the surest way to honour that is to make an unfiltered read
 * impossible to write by accident: there is no {@code findById} here that a
 * future endpoint could reach for.
 *
 * <p>The joins are explicit rather than an entity association. The two tables
 * are one-to-one and the projections need a handful of columns, so selecting
 * them directly avoids both a mapped association between two feature packages
 * and a second query per row.
 */
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

	@Query("""
			select new ro.garajulmeu.vehicle.dto.VehicleSummary(
				v.id, v.displayName, c.registrationNumber, c.make, c.commercialDescription)
			from Vehicle v
			join RegistrationCertificate c on c.vehicleId = v.id
			where v.userId = :userId
			order by c.registrationNumber
			""")
	List<VehicleSummary> summariesOf(@Param("userId") UUID userId);

	@Query("""
			select new ro.garajulmeu.vehicle.dto.VehicleDetails(
				v.id, v.displayName, c.registrationNumber, c.make, c.commercialDescription,
				c.vin, v.createdAt)
			from Vehicle v
			join RegistrationCertificate c on c.vehicleId = v.id
			where v.id = :vehicleId and v.userId = :userId
			""")
	Optional<VehicleDetails> detailsOf(@Param("vehicleId") UUID vehicleId, @Param("userId") UUID userId);
}