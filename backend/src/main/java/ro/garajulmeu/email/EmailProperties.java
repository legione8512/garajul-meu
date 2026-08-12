package ro.garajulmeu.email;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Email configuration. Today it only declares which provider implementation is
 * active; the Resend API key, sender address and reply-to arrive with the real
 * provider later in this phase.
 *
 * <p>Declaring the property here is not ceremony: it is what makes the key
 * appear in IDE completion and validation, and it gives the setting one
 * documented home instead of a bare string compared inside an annotation.
 *
 * @param provider {@code logging} writes messages to the log for local
 *                 development; {@code resend} delivers them for real
 */
@ConfigurationProperties(prefix = "garajul-meu.email")
public record EmailProperties(@DefaultValue("logging") String provider) {
}