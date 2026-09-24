package ro.garajulmeu.feedback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param category   IDEA, PROBLEM or FEATURE
 * @param platform   WEB, ANDROID or IOS, as the client reports itself
 * @param appVersion the installed application's version; absent on the web
 */
public record SendFeedbackRequest(

		@NotBlank String category,

		@NotBlank @Size(max = 2000) String message,

		@Size(max = 16) String platform,

		@Size(max = 32) String appVersion) {
}
