package ro.garajulmeu.email;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Email configuration.
 *
 * <p>Declaring these here is not ceremony: it is what makes the keys appear in
 * IDE completion and validation, and it gives each setting one documented home
 * instead of a bare string compared inside an annotation.
 *
 * @param provider {@code logging} writes messages to the log for local
 *                 development; {@code resend} delivers them for real
 * @param apiKey   the Resend key, from the environment and never from a file -
 *                 the repository is public. Unused by the logging provider, so
 *                 it has no default and {@link ResendEmailProvider} refuses to
 *                 start without it rather than failing on the first
 *                 registration, hours later, in front of a real person
 * @param from     the sender, in either bare or {@code Name <address>} form. Its
 *                 domain is what Resend verifies, and section 21's topology puts
 *                 that on a sending subdomain rather than the root: a
 *                 verification code marked as spam then damages the reputation
 *                 of mail nobody reads, not of the address a person writes to
 * @param baseUrl  overridable only so a test can point somewhere that is not the
 *                 internet
 */
@ConfigurationProperties(prefix = "garajul-meu.email")
public record EmailProperties(
		@DefaultValue("logging") String provider,
		String apiKey,
		String from,
		@DefaultValue("https://api.resend.com") String baseUrl) {
}