package ro.garajulmeu.vehiclepayment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Every query matches the vehicle and the account in SQL, as the documents'
 * repository does: knowing a payment's UUID is never enough to read it.
 */
public interface VehiclePaymentRepository extends JpaRepository<VehiclePayment, UUID> {

	@Query("""
			select p from VehiclePayment p
			join Vehicle v on v.id = p.vehicleId
			where p.vehicleId = :vehicleId and v.userId = :userId
			order by p.firstDueDate, p.createdAt
			""")
	List<VehiclePayment> ofVehicle(@Param("vehicleId") UUID vehicleId, @Param("userId") UUID userId);

	@Query("""
			select p from VehiclePayment p
			join Vehicle v on v.id = p.vehicleId
			where p.id = :paymentId and p.vehicleId = :vehicleId and v.userId = :userId
			""")
	Optional<VehiclePayment> byIdOfVehicle(@Param("paymentId") UUID paymentId,
			@Param("vehicleId") UUID vehicleId, @Param("userId") UUID userId);

	/** Every payment in the garage, for the dashboard and for a preferences change. */
	@Query("""
			select p from VehiclePayment p
			join Vehicle v on v.id = p.vehicleId
			where v.userId = :userId
			""")
	List<VehiclePayment> ofGarage(@Param("userId") UUID userId);
}
