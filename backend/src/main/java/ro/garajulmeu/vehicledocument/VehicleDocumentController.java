package ro.garajulmeu.vehicledocument;

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

import ro.garajulmeu.vehicledocument.dto.DocumentDetails;
import ro.garajulmeu.vehicledocument.dto.SaveDocumentRequest;

/**
 * Specification section 16's five document endpoints. Renewal is 10.3 and
 * history is 10.5; both are separate paths rather than modes of these.
 *
 * <p>Two identifiers in the path, and both are matched against the account in
 * SQL rather than compared afterwards - the vehicle because it is the caller's
 * way in, and the document because knowing a UUID must never be enough.
 */
@RestController
@RequestMapping("/api/v1/vehicles/{vehicleId}/documents")
public class VehicleDocumentController {

	private final VehicleDocumentService documentService;

	VehicleDocumentController(VehicleDocumentService documentService) {
		this.documentService = documentService;
	}

	@GetMapping
	public List<DocumentDetails> documents(@AuthenticationPrincipal Jwt token,
			@PathVariable UUID vehicleId) {
		return documentService.documentsOf(accountOf(token), vehicleId);
	}

	@GetMapping("/{documentId}")
	public DocumentDetails details(@AuthenticationPrincipal Jwt token,
			@PathVariable UUID vehicleId, @PathVariable UUID documentId) {
		return documentService.detailsOf(accountOf(token), vehicleId, documentId);
	}

	/** 201 with the created document, and no Location header, as vehicles decided. */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public DocumentDetails add(@AuthenticationPrincipal Jwt token, @PathVariable UUID vehicleId,
			@Valid @RequestBody SaveDocumentRequest request) {
		return documentService.add(accountOf(token), vehicleId, request);
	}

	@PatchMapping("/{documentId}")
	public DocumentDetails correct(@AuthenticationPrincipal Jwt token,
			@PathVariable UUID vehicleId, @PathVariable UUID documentId,
			@Valid @RequestBody SaveDocumentRequest request) {
		return documentService.correct(accountOf(token), vehicleId, documentId, request);
	}

	@DeleteMapping("/{documentId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@AuthenticationPrincipal Jwt token, @PathVariable UUID vehicleId,
			@PathVariable UUID documentId) {
		documentService.delete(accountOf(token), vehicleId, documentId);
	}

	private static UUID accountOf(Jwt token) {
		return UUID.fromString(token.getSubject());
	}
}