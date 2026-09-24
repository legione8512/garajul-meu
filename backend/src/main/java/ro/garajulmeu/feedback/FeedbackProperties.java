package ro.garajulmeu.feedback;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Where the Sugestii tab's messages are sent, and how many one account may send.
 *
 * @param recipient  the operator's address - the public contact address, which
 *                   the privacy policy and the store listings already publish
 * @param dailyLimit messages per account in any twenty-four hours; five, by the
 *                   owner's decision
 */
@ConfigurationProperties(prefix = "garajul-meu.feedback")
public record FeedbackProperties(
		@DefaultValue("in.garaj.meu@gmail.com") String recipient,
		@DefaultValue("5") int dailyLimit) {
}
