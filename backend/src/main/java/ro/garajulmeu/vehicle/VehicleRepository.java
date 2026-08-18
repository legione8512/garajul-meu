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
 * <p><strong>Every finder declared here takes the account id and matches it in
 * SQL.</strong> Section 15 says knowing a UUID must never be enough to reach
 * another account's data, and a service that has no unscoped finder to call
 * cannot forget to scope one. Note the limit of that claim: {@code JpaRepository}
 * still inherits {@code findById}, so this is a convention rather than something
 * the compiler enforces.
 *
 * <p>The joins are explicit rather than an entity association. The two tables
 * are one-to-one and the projections need a handful of columns, so selecting
 * them directly avoids both a mapped association between two feature packages
 * and a second query per row.
 */
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

	Optional<Vehicle> findByIdAndUserId(UUID id, UUID userId);

	@Query("""
			select new ro.garajulmeu.vehicle.dto.VehicleSummary(
				v.id, v.displayName, c.registrationNumber, c.make, c.commercialDescription)
			from Vehicle v
			join RegistrationCertificate c on c.vehicleId = v.id
			where v.userId = :userId
			order by c.registrationNumber
			""")
	List<VehicleSummary> summariesOf(@Param("userId") UUID userId);

	/**
	 * The {@code case} is how JPQL says "is there one" without selecting the key
	 * itself. Sending the key would put an object identifier in every vehicle
	 * response for no client that reads it; sending the boolean answers the only
	 * question a screen has.
	 */
	@Query("""
			select new ro.garajulmeu.vehicle.dto.VehicleDetails(
				v.id, v.displayName, c.registrationNumber, c.make, c.commercialDescription,
				c.vin, v.createdAt,
				case when v.imageObjectKey is not null then true else false end)
			from Vehicle v
			join RegistrationCertificate c on c.vehicleId = v.id
			where v.id = :vehicleId and v.userId = :userId
			""")
	Optional<VehicleDetails> detailsOf(@Param("vehicleId") UUID vehicleId, @Param("userId") UUID userId);

	/**
	 * Every stored photograph in one account's garage, for the moment before that
	 * account ceases to exist.
	 *
	 * <p>Keys rather than vehicles, because keys are all the caller needs and this
	 * is the one column in the table that means nothing to anybody outside the
	 * backend. Rows with no photograph are excluded in SQL rather than filtered in
	 * Java: a garage of ten cars and one picture should return one string.
	 */
	@Query("""
			select v.imageObjectKey
			from Vehicle v
			where v.userId = :userId and v.imageObjectKey is not null
			""")
	List<String> imageKeysOf(@Param("userId") UUID userId);
}