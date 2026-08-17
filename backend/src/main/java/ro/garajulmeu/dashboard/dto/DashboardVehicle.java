package ro.garajulmeu.dashboard.dto;

import java.util.List;
import java.util.UUID;

/**
 * One vehicle on the dashboard, named as the garage names it and followed by its
 * four document lines in the order {@code DocumentType} declares them.
 */
public record DashboardVehicle(
		UUID vehicleId,
		String displayName,
		String registrationNumber,
		String make,
		String commercialDescription,
		List<DocumentStatusLine> documents) {
}