package ro.garajulmeu.common;

import java.util.List;

/**
 * One page of anything, and the shape every paginated endpoint in this API
 * answers with.
 *
 * <p>Deliberately not Spring Data's {@code Page}. Serialising {@code PageImpl}
 * produces a JSON contract Spring itself warns is unstable across versions -
 * fields have moved and been renamed between releases - and a mobile client
 * built against one shape cannot be updated as quickly as a server. This record
 * is ours, so it changes when we decide it does.
 *
 * <p>{@code totalElements} is a {@code long} because a count is; {@code page} is
 * zero-based, as every pagination this project will meet is.
 */
public record PageResponse<T>(
		List<T> items,
		int page,
		int size,
		long totalElements,
		int totalPages) {
}