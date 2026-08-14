package ro.garajulmeu.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A partial update: every field is optional, and an absent one is left alone.
 *
 * <p>That is why nothing here is {@code @NotBlank} - absent must stay legal. The
 * pattern on {@code fullName} does the work {@code @NotBlank} would have done
 * without forbidding absence: Bean Validation skips a null, so only a value that
 * is actually present has to contain a non-whitespace character.
 *
 * @param timezone an IANA identifier. Its membership of the real set of zones
 *                 cannot be expressed declaratively without a custom validator,
 *                 so the service checks it
 */
public record UpdateProfileRequest(

		@Size(max = 120)
		@Pattern(regexp = ".*\\S.*")
		String fullName,

		@Pattern(regexp = "ro|en")
		String preferredLanguage,

		@Size(max = 64)
		String timezone) {
}