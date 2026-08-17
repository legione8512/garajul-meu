package ro.garajulmeu.vehicledocument;

import java.time.LocalDate;
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
	 * The records covering the vehicle on a given day, best first. Section 11
	 * defines "covering" as {@code valid_from <= today <= valid_until} with a
	 * missing start treated as already effective, and resolves an overlap by the
	 * greatest start date and then the newest row.
	 *
	 * <p>Answers a list rather than one row on purpose: overlaps are legal, and a
	 * finder that returned {@code Optional} would throw on data the specification
	 * permits. The caller takes the first.
	 */
	@Query("""
			select d from VehicleDocument d
			join Vehicle v on v.id = d.vehicleId
			where d.vehicleId = :vehicleId and v.userId = :userId and d.type = :type
				and (d.validFrom is null or d.validFrom <= :today)
				and d.validUntil >= :today
			order by d.validFrom desc nulls last, d.createdAt desc
			""")
	List<VehicleDocument> coveringOn(@Param("vehicleId") UUID vehicleId, @Param("userId") UUID userId,
			@Param("type") DocumentType type, @Param("today") LocalDate today);
}