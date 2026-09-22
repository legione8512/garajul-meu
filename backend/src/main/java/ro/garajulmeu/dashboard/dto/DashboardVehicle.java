package ro.garajulmeu.dashboard.dto;

import java.util.List;
import java.util.UUID;

/**
 * One vehicle on the dashboard, named as the garage names it and followed by its
 * document lines in the order {@code DocumentType} declares them.
 *
 * <p>{@code hasImage} is the garage's field, meaning the same thing for the same
 * reason: since 1.0.2 the card draws the owner's photograph, and this is what
 * tells it whether to ask for one.
 */
public record DashboardVehicle(
		UUID vehicleId,
		String displayName,
		String registrationNumber,
		String make,
		String commercialDescription,
		boolean hasImage,
		List<DocumentStatusLine> documents) {
}