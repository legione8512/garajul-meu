package ro.garajulmeu.feedback;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import ro.garajulmeu.TestcontainersConfiguration;
import ro.garajulmeu.email.EmailProvider;
import ro.garajulmeu.security.AccessTokenService;
import ro.garajulmeu.user.User;
import ro.garajulmeu.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The Sugestii tab's endpoint, 1.1.
 *
 * <p>Annotations identical to AuthFlowTest, so the cached context is reused and
 * no further container starts - and here the EmailProvider mock is not unused:
 * it is how these tests see what would have been emailed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class FeedbackFlowTest {

	private static final String PATH = "/api/v1/feedback";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private FeedbackRepository feedbackRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AccessTokenService accessTokenService;

	@MockitoBean
	private EmailProvider emailProvider;

	private record Account(UUID id, String token) {
	}

	private Account givenAccount(String email) {
		User user = new User("Ana Pop", email, passwordEncoder.encode("a-long-enough-password"));
		user.setEmailVerifiedAt(Instant.now());
		UUID id = userRepository.saveAndFlush(user).getId();
		return new Account(id, accessTokenService.issueFor(id).value());
	}

	private ResultActions send(Account account, String body) throws Exception {
		return mockMvc.perform(post(PATH)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + account.token())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private static String message(String category, String text) {
		return """
				{"category":"%s","message":"%s","platform":"android","appVersion":"1.1.0"}
				""".formatted(category, text);
	}

	@Test
	void aMessageIsStoredAndEmailedWithTheSenderAsReplyTo() throws Exception {
		Account account = givenAccount("feedback@example.com");

		send(account, message("idea", "  Un calendar al reviziilor  "))
				.andExpect(status().isNoContent());

		assertThat(feedbackRepository.findByUserIdOrderByCreatedAt(account.id()))
				.singleElement()
				.satisfies(saved -> {
					assertThat(saved.getCategory()).isEqualTo(FeedbackCategory.IDEA);
					assertThat(saved.getMessage()).isEqualTo("Un calendar al reviziilor");
					assertThat(saved.getPlatform()).isEqualTo("ANDROID");
					assertThat(saved.getAppVersion()).isEqualTo("1.1.0");
				});

		verify(emailProvider).sendFeedback(eq("in.garaj.meu@gmail.com"), eq("feedback@example.com"),
				eq("Garajul Meu — Idee: Un calendar al reviziilor"), anyString());
	}

	/** The message is in the database, so the person is told it arrived - which is true. */
	@Test
	void aMailThatFailsDoesNotLoseTheMessage() throws Exception {
		Account account = givenAccount("mailfails@example.com");
		doThrow(new IllegalStateException("Resend is down")).when(emailProvider)
				.sendFeedback(anyString(), anyString(), anyString(), anyString());

		send(account, message("PROBLEM", "Nu merge ceva")).andExpect(status().isNoContent());

		assertThat(feedbackRepository.findByUserIdOrderByCreatedAt(account.id())).hasSize(1);
	}

	@Test
	void theSixthMessageInADayIsRefusedAndNotEmailed() throws Exception {
		Account account = givenAccount("chatty@example.com");

		for (int sent = 1; sent <= 5; sent++) {
			send(account, message("IDEA", "Idee " + sent)).andExpect(status().isNoContent());
		}

		send(account, message("IDEA", "Idee 6"))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.code").value("FEEDBACK_LIMIT_REACHED"));

		assertThat(feedbackRepository.findByUserIdOrderByCreatedAt(account.id())).hasSize(5);
		verify(emailProvider, never()).sendFeedback(anyString(), anyString(), startsWith("Garajul Meu — Idee: Idee 6"),
				anyString());
	}

	/** One account's allowance is its own. */
	@Test
	void theAllowanceIsCountedPerAccount() throws Exception {
		Account first = givenAccount("first@example.com");
		Account second = givenAccount("second@example.com");

		for (int sent = 1; sent <= 5; sent++) {
			send(first, message("IDEA", "Idee " + sent)).andExpect(status().isNoContent());
		}

		send(second, message("IDEA", "A mea")).andExpect(status().isNoContent());
	}

	@Test
	void anUnknownCategoryAnEmptyOrAnOverlongMessageIsRefused() throws Exception {
		Account account = givenAccount("refused@example.com");

		send(account, message("COMPLAINT", "text"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		send(account, message("IDEA", "   "))
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		send(account, message("IDEA", "a".repeat(2001)))
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		assertThat(feedbackRepository.findByUserIdOrderByCreatedAt(account.id())).isEmpty();
	}

	/** A platform this project does not ship to is recorded as unknown, version and all. */
	@Test
	void anUnknownPlatformIsRecordedAsUnknown() throws Exception {
		Account account = givenAccount("platform@example.com");

		send(account, """
				{"category":"IDEA","message":"x","platform":"windows","appVersion":"9"}
				""").andExpect(status().isNoContent());

		FeedbackMessage saved = feedbackRepository.findByUserIdOrderByCreatedAt(account.id()).get(0);
		assertThat(saved.getPlatform()).isNull();
		assertThat(saved.getAppVersion()).isNull();
	}

	@Test
	void aSignedOutCallerCannotSendAnything() throws Exception {
		mockMvc.perform(post(PATH)
						.contentType(MediaType.APPLICATION_JSON)
						.content(message("IDEA", "spam")))
				.andExpect(status().isUnauthorized());
	}

	/** V14 cascades: a deleted account's messages go with it. */
	@Test
	void anAccountsMessagesGoWithTheAccount() throws Exception {
		Account account = givenAccount("leaving@example.com");
		send(account, message("IDEA", "La revedere")).andExpect(status().isNoContent());

		userRepository.deleteById(account.id());
		userRepository.flush();

		assertThat(feedbackRepository.findByUserIdOrderByCreatedAt(account.id())).isEmpty();
	}
}
