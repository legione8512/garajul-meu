package ro.garajulmeu.vehicle;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ro.garajulmeu.vehicle.dto.CreateVehicleRequest;
import ro.garajulmeu.vehicle.dto.UpdateVehicleRequest;
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
		return vehicleService.garageOf(accountOf(token));
	}

	/**
	 * The first endpoint in the project with an identifier in the path, and so
	 * the first that can be pointed at somebody else's data. The account comes
	 * from the token and the vehicle from the path, and the two are matched in
	 * the query rather than compared afterwards.
	 */
	@GetMapping("/{vehicleId}")
	public VehicleDetails details(@AuthenticationPrincipal Jwt token, @PathVariable UUID vehicleId) {
		return vehicleService.detailsOf(accountOf(token), vehicleId);
	}

	/**
	 * 201 with the created vehicle in the body, and deliberately no Location
	 * header: a browser cannot read one cross-origin unless it is added to the
	 * CORS exposed headers, and the identifier is already in the body the client
	 * has to parse anyway. Exposing a second header to carry the same value
	 * would be configuration nobody consumes.
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public VehicleDetails create(@AuthenticationPrincipal Jwt token,
			@Valid @RequestBody CreateVehicleRequest request) {
		return vehicleService.create(accountOf(token), request);
	}

	@PatchMapping("/{vehicleId}")
	public VehicleDetails rename(@AuthenticationPrincipal Jwt token, @PathVariable UUID vehicleId,
			@Valid @RequestBody UpdateVehicleRequest request) {
		return vehicleService.rename(accountOf(token), vehicleId, request);
	}

	@DeleteMapping("/{vehicleId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@AuthenticationPrincipal Jwt token, @PathVariable UUID vehicleId) {
		vehicleService.delete(accountOf(token), vehicleId);
	}

	private static UUID accountOf(Jwt token) {
		return UUID.fromString(token.getSubject());
	}
}