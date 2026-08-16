package ro.garajulmeu.registrationcertificate;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ro.garajulmeu.registrationcertificate.dto.CertificateData;

/** The paths and verbs fixed by the section 16 API contract. */
@RestController
@RequestMapping("/api/v1/vehicles/{vehicleId}/registration-certificate")
public class RegistrationCertificateController {

	private final RegistrationCertificateService certificateService;

	RegistrationCertificateController(RegistrationCertificateService certificateService) {
		this.certificateService = certificateService;
	}

	@GetMapping
	public CertificateData read(@AuthenticationPrincipal Jwt token, @PathVariable UUID vehicleId) {
		return certificateService.of(accountOf(token), vehicleId);
	}

	@PatchMapping
	public CertificateData correct(@AuthenticationPrincipal Jwt token, @PathVariable UUID vehicleId,
			@Valid @RequestBody CertificateData data) {
		return certificateService.correct(accountOf(token), vehicleId, data);
	}

	private static UUID accountOf(Jwt token) {
		return UUID.fromString(token.getSubject());
	}
}