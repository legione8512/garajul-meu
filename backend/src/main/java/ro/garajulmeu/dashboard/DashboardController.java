package ro.garajulmeu.dashboard;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ro.garajulmeu.dashboard.dto.DashboardView;

/**
 * Specification section 16's dashboard projection. It takes no parameters at all
 * - not even a vehicle - because the only garage it can describe is the token's.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

	private final DashboardService dashboardService;

	DashboardController(DashboardService dashboardService) {
		this.dashboardService = dashboardService;
	}

	@GetMapping
	public DashboardView dashboard(@AuthenticationPrincipal Jwt token) {
		return dashboardService.of(UUID.fromString(token.getSubject()));
	}
}