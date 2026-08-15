package ro.garajulmeu.vehicle;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ro.garajulmeu.vehicle.dto.VehicleDetails;
import ro.garajulmeu.vehicle.dto.VehicleSummary;

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {

	private final VehicleService vehicleService;

	VehicleController(VehicleService vehicleService) {
		this.vehicleService = vehicleService;
	}

	/** The garage of the token's account. There is no way to ask for another. */
	@GetMapping
	public List<VehicleSummary> garage(@AuthenticationPrincipal Jwt token) {
		return vehicleService.garageOf(UUID.fromString(token.getSubject()));
	}

	/**
	 * The first endpoint in the project with an identifier in the path, and so
	 * the first that can be pointed at somebody else's data. The account comes
	 * from the token and the vehicle from the path, and the two are matched in
	 * the query rather than compared afterwards.
	 */
	@GetMapping("/{vehicleId}")
	public VehicleDetails details(@AuthenticationPrincipal Jwt token, @PathVariable UUID vehicleId) {
		return vehicleService.detailsOf(UUID.fromString(token.getSubject()), vehicleId);
	}
}