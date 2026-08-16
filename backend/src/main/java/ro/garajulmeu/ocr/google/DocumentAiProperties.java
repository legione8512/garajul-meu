package ro.garajulmeu.ocr.google;

import java.util.Locale;

import com.google.cloud.documentai.v1.ProcessorName;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Which processor to call. Three identifiers, none of them secret: they name a
 * resource, they do not open it - that is what the credentials in the
 * environment are for, and why these can sit in a file this repository publishes.
 *
 * <p>Bound whatever the active provider is, so with the stub they are simply
 * null and nothing reads them. **The checks live in the methods rather than in
 * the constructor** for exactly that reason: a missing value must fail the
 * google path loudly and leave the stub path alone.
 */
@ConfigurationProperties(prefix = "garajul-meu.ocr.google")
public record DocumentAiProperties(String projectId, String location, String processorId) {

	/**
	 * The regional endpoint, and it is not optional. A processor created in the
	 * EU is invisible to the global endpoint: the call answers NOT_FOUND, which
	 * reads as "no such processor" when the truth is "you asked the wrong
	 * building".
	 */
	String endpoint() {
		return normalisedLocation() + "-documentai.googleapis.com:443";
	}

	String processorName() {
		return ProcessorName.of(
				required(projectId, "project-id"),
				normalisedLocation(),
				required(processorId, "processor-id")).toString();
	}

	/**
	 * The console shows the region as {@code EU} and the API wants {@code eu}.
	 * Copying what is on screen is the obvious thing to do and produces a
	 * NOT_FOUND with no hint as to why, so it is normalised here rather than
	 * trusted to whoever fills in the file.
	 */
	private String normalisedLocation() {
		return required(location, "location").toLowerCase(Locale.ROOT);
	}

	private static String required(String value, String property) {
		if (value == null || value.isBlank()) {
			throw new IllegalStateException(
					"garajul-meu.ocr.google." + property + " must be set when the OCR provider is google");
		}
		return value;
	}
}