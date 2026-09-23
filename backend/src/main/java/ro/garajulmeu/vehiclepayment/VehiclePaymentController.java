package ro.garajulmeu.vehiclepayment;

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

import ro.garajulmeu.vehiclepayment.dto.PaymentDetails;
import ro.garajulmeu.vehiclepayment.dto.SavePaymentRequest;

/**
 * A vehicle's recurring payments, 1.1. Under the vehicle as its documents are,
 * with both identifiers matched against the account in SQL.
 */
@RestController
@RequestMapping("/api/v1/vehicles/{vehicleId}/payments")
public class VehiclePaymentController {

	private final VehiclePaymentService paymentService;

	VehiclePaymentController(VehiclePaymentService paymentService) {
		this.paymentService = paymentService;
	}

	@GetMapping
	public List<PaymentDetails> payments(@AuthenticationPrincipal Jwt token,
			@PathVariable UUID vehicleId) {
		return paymentService.paymentsOf(accountOf(token), vehicleId);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PaymentDetails add(@AuthenticationPrincipal Jwt token, @PathVariable UUID vehicleId,
			@Valid @RequestBody SavePaymentRequest request) {
		return paymentService.add(accountOf(token), vehicleId, request);
	}

	@PatchMapping("/{paymentId}")
	public PaymentDetails correct(@AuthenticationPrincipal Jwt token,
			@PathVariable UUID vehicleId, @PathVariable UUID paymentId,
			@Valid @RequestBody SavePaymentRequest request) {
		return paymentService.correct(accountOf(token), vehicleId, paymentId, request);
	}

	@DeleteMapping("/{paymentId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@AuthenticationPrincipal Jwt token, @PathVariable UUID vehicleId,
			@PathVariable UUID paymentId) {
		paymentService.delete(accountOf(token), vehicleId, paymentId);
	}

	private static UUID accountOf(Jwt token) {
		return UUID.fromString(token.getSubject());
	}
}
