package ro.garajulmeu.dashboard.dto;

import java.util.List;

/**
 * An object rather than a bare array. {@code /api/v1/dashboard} names a single
 * resource, and a resource that answers with a list of something else has
 * nowhere to put a second fact when one arrives - a count, a generated-at
 * timestamp, a warning that a reminder could not be sent.
 */
public record DashboardView(List<DashboardVehicle> vehicles) {
}