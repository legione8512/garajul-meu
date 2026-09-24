package ro.garajulmeu.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import ro.garajulmeu.auth.AuthProperties;
import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.user.Language;

/**
 * No Spring context and no network: MockRestServiceServer replaces the request
 * factory on the builder the provider is handed, so the assertions are about
 * the exact HTTP request Resend would have received.
 */
class ResendEmailProviderTest {

	private static final String KEY = "re_a_test_key_that_opens_nothing";

	private static final String FROM = "Garajul Meu <noreply@mail.cyber-half.com>";

	private final EmailMessages messages = new EmailMessages(
			new AuthProperties(Duration.ofMinutes(15), 5, Duration.ofDays(30), Duration.ofSeconds(15)));

	private final RestClient.Builder builder = RestClient.builder();

	private final MockRestServiceServer resend = MockRestServiceServer.bindTo(builder).build();

	private ResendEmailProvider provider() {
		return new ResendEmailProvider(new EmailProperties("resend", KEY, FROM, "https://api.resend.com"), messages,
				builder);
	}

	@Test
	void sendsTheDocumentedRequestToResend() {
		resend.expect(requestTo("https://api.resend.com/emails")).andExpect(method(HttpMethod.POST))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + KEY))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.from").value(FROM)).andExpect(jsonPath("$.to[0]").value("marius@example.com"))
				.andExpect(jsonPath("$.subject").value(containsString("Garajul Meu")))
				.andExpect(jsonPath("$.text").value(containsString("602431"))).andRespond(
						withSuccess("{\"id\":\"49a3999c-0ce1-4ea6-ab68-afcd6dc2e794\"}", MediaType.APPLICATION_JSON));

		provider().sendVerificationCode("marius@example.com", "602431", Language.RO);

		resend.verify();
	}

	/**
	 * The recipient is the address on file; the requested one appears in the body.
	 */
	@Test
	void anEmailChangeGoesToTheCurrentAddressAndNamesTheRequestedOne() {
		resend.expect(requestTo("https://api.resend.com/emails"))
				.andExpect(jsonPath("$.to[0]").value("acum@example.com"))
				.andExpect(jsonPath("$.text").value(containsString("nou@example.com")))
				.andRespond(withSuccess("{\"id\":\"x\"}", MediaType.APPLICATION_JSON));

		provider().sendEmailChangeCode("acum@example.com", "nou@example.com", "602431", Language.RO);

		resend.verify();
	}

	/**
	 * Not swallowed, deliberately. Registration sends inside its transaction, so
	 * the exception is what rolls the account back - a caught failure would leave
	 * somebody holding an account they can never verify, and the log would be the
	 * only place it was ever mentioned.
	 */
	@Test
	void aFailureAtResendReachesTheCaller() {
		resend.expect(requestTo("https://api.resend.com/emails")).andRespond(withServerError());

		assertThatThrownBy(() -> provider().sendVerificationCode("marius@example.com", "602431", Language.RO))
				.isInstanceOf(RestClientException.class);
	}

	/**
	 * Resend's answer to Google Play's pre-launch robot on 2026-09-19, byte for
	 * byte. The address is what the person typed, so it is their mistake to be
	 * told about - not an outage to be paged about - and the exception names the
	 * message that was refused without naming the address.
	 */
	@Test
	void anAddressResendWillNotSendToIsThePersonsMistake() {
		resend.expect(requestTo("https://api.resend.com/emails"))
				.andRespond(withStatus(HttpStatus.UNPROCESSABLE_CONTENT).contentType(MediaType.APPLICATION_JSON)
						.body("{\"statusCode\":422,\"name\":\"validation_error\",\"message\":\"Invalid `to` field. "
								+ "Please use our testing email address instead of domains like `example.com`. "
								+ "See our documentation for more information.\"}"));

		assertThatThrownBy(() -> provider().sendVerificationCode("robot@example.com", "602431", Language.RO))
				.isInstanceOf(EmailRecipientRejectedException.class).hasMessageContaining("verification code")
				.hasMessageNotContaining("robot@example.com");
	}

	/**
	 * A 422 under another name is about this side's request - here the sender,
	 * which is configuration - and must stay a failure that reaches Sentry.
	 */
	@Test
	void aRefusalOfTheSenderIsStillAFailureOfOurs() {
		resend.expect(requestTo("https://api.resend.com/emails"))
				.andRespond(withStatus(HttpStatus.UNPROCESSABLE_CONTENT).contentType(MediaType.APPLICATION_JSON)
						.body("{\"statusCode\":422,\"name\":\"invalid_from_address\",\"message\":\"Invalid `from` field.\"}"));

		assertThatThrownBy(() -> provider().sendVerificationCode("marius@example.com", "602431", Language.RO))
				.isInstanceOf(HttpClientErrorException.class)
				.isNotInstanceOf(EmailRecipientRejectedException.class);
	}

	/** A 422 whose body cannot be read proves nothing about the recipient. */
	@Test
	void anUnreadableRefusalIsStillAFailureOfOurs() {
		resend.expect(requestTo("https://api.resend.com/emails"))
				.andRespond(withStatus(HttpStatus.UNPROCESSABLE_CONTENT).contentType(MediaType.TEXT_PLAIN)
						.body("unprocessable"));

		assertThatThrownBy(() -> provider().sendVerificationCode("marius@example.com", "602431", Language.RO))
				.isInstanceOf(HttpClientErrorException.class);
	}

	/**
	 * Why the refusal is its own type. Confirming an email change sends to the
	 * new address and runs with {@code noRollbackFor = ApiException.class}; as an
	 * ApiException, a refused new address would be committed to the account
	 * while the caller was told the change had failed.
	 */
	@Test
	void theRefusalRollsBackWhereABusinessFailureWouldNot() {
		assertThat(RuntimeException.class.isAssignableFrom(EmailRecipientRejectedException.class)).isTrue();
		assertThat(ApiException.class.isAssignableFrom(EmailRecipientRejectedException.class)).isFalse();
	}

	/**
	 * At startup, not at the first registration. The shipped configuration writes
	 * {@code ${RESEND_API_KEY:}}, so an unset variable arrives as an empty string -
	 * which would otherwise send a request Resend refuses, in production, for a
	 * reason nobody would trace back to a missing variable.
	 */
	@Test
	void refusesToStartWithoutTheSettingsItCannotSendWithout() {
		assertThatThrownBy(
				() -> new ResendEmailProvider(new EmailProperties("resend", "", FROM, "https://api.resend.com"),
						messages, builder))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("api-key");

		assertThatThrownBy(
				() -> new ResendEmailProvider(new EmailProperties("resend", KEY, null, "https://api.resend.com"),
						messages, builder))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("from");
	}

	/** 1.1: a Sugestii message goes to the operator, and answering it answers the sender. */
	@Test
	void aFeedbackMessageCarriesTheSenderAsReplyTo() {
		resend.expect(requestTo("https://api.resend.com/emails")).andExpect(method(HttpMethod.POST))
				.andExpect(jsonPath("$.to[0]").value("in.garaj.meu@gmail.com"))
				.andExpect(jsonPath("$.reply_to").value("ana@example.com"))
				.andExpect(jsonPath("$.subject").value("Garajul Meu — Idee: un calendar"))
				.andExpect(jsonPath("$.text").value("corpul"))
				.andRespond(withSuccess("{\"id\":\"f1\"}", MediaType.APPLICATION_JSON));

		provider().sendFeedback("in.garaj.meu@gmail.com", "ana@example.com",
				"Garajul Meu — Idee: un calendar", "corpul");

		resend.verify();
	}

	/** The account's own emails have nobody to reply to, and say nothing about it. */
	@Test
	void anAccountEmailSendsNoReplyTo() {
		resend.expect(requestTo("https://api.resend.com/emails"))
				.andExpect(jsonPath("$.reply_to").doesNotExist())
				.andRespond(withSuccess("{\"id\":\"a1\"}", MediaType.APPLICATION_JSON));

		provider().sendVerificationCode("marius@example.com", "602431", Language.RO);

		resend.verify();
	}
}
