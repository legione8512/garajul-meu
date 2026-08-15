package ro.garajulmeu.vehicle;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;
import ro.garajulmeu.vehicle.dto.VehicleDetails;
import ro.garajulmeu.vehicle.dto.VehicleSummary;

@Service
public class VehicleService {

	private final VehicleRepository vehicleRepository;

	VehicleService(VehicleRepository vehicleRepository) {
		this.vehicleRepository = vehicleRepository;
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
}