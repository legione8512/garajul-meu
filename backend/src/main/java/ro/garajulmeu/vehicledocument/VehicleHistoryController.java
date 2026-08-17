package ro.garajulmeu.vehicledocument;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ro.garajulmeu.common.PageResponse;
import ro.garajulmeu.vehicledocument.dto.DocumentDetails;

/**
 * A controller of its own rather than a sixth method on
 * {@code VehicleDocumentController}: section 16 puts history on its own path,
 * beside {@code /documents} rather than under it, and a class-level mapping can
 * only say one of those.
 */
@RestController
@RequestMapping("/api/v1/vehicles/{vehicleId}/history")
public class VehicleHistoryController {

	private final VehicleDocumentService documentService;

	VehicleHistoryController(VehicleDocumentService documentService) {
		this.documentService = documentService;
	}

	@GetMapping
	public PageResponse<DocumentDetails> history(@AuthenticationPrincipal Jwt token,
			@PathVariable UUID vehicleId,
			@RequestParam(required = false) String type,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size) {
		return documentService.historyOf(UUID.fromString(token.getSubject()), vehicleId, type, page, size);
	}
}