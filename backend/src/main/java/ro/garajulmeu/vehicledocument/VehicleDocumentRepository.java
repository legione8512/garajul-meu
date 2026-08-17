package ro.garajulmeu.vehicledocument;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * <p><strong>Every finder takes the account id and matches it in SQL</strong>,
 * as in {@code VehicleRepository} and for the same reason: section 15 says
 * knowing a UUID must never be enough to reach another account's data. The
 * documents table carries no owner of its own, so ownership is reached by
 * joining to the vehicle - the certificate's denormalised owner exists to serve
 * a uniqueness constraint that has no counterpart here.
 *
 * <p><strong>There is deliberately no "which record covers today" query.</strong>
 * There was one, briefly. Section 11's selection rule now lives in
 * {@link DocumentCoverage} in Java and only there: the dashboard needs a whole
 * garage at once, so a per-pair query would be one round trip per vehicle per
 * type, and keeping both would leave one rule in two languages to drift apart.
 */
public interface VehicleDocumentRepository extends JpaRepository<VehicleDocument, UUID> {

	/** Newest expiry first, so a renewal sits above the record it replaced. */
	@Query("""
			select d from VehicleDocument d
			join Vehicle v on v.id = d.vehicleId
			where d.vehicleId = :vehicleId and v.userId = :userId
			order by d.validUntil desc, d.createdAt desc
			""")
	List<VehicleDocument> ofVehicle(@Param("vehicleId") UUID vehicleId, @Param("userId") UUID userId);

	@Query("""
			select d from VehicleDocument d
			join Vehicle v on v.id = d.vehicleId
			where d.id = :documentId and d.vehicleId = :vehicleId and v.userId = :userId
			""")
	Optional<VehicleDocument> byIdOfVehicle(@Param("documentId") UUID documentId,
			@Param("vehicleId") UUID vehicleId, @Param("userId") UUID userId);

	/**
	 * Every document of every vehicle in one account, in one round trip. This is
	 * what keeps the dashboard at three queries whatever the garage holds: the
	 * grouping by vehicle and by type is done in memory, over the few dozen rows
	 * an account has, rather than asked of the database once per pair.
	 */
	@Query("""
			select d from VehicleDocument d
			join Vehicle v on v.id = d.vehicleId
			where v.userId = :userId
			""")
	List<VehicleDocument> ofGarage(@Param("userId") UUID userId);
}