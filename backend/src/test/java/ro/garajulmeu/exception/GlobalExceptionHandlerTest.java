package ro.garajulmeu.exception;

import static org.hamcrest.Matchers.containsString;
import ro.garajulmeu.common.RequestIdFilter;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.security.test.context.support.WithMockUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Uses a throwaway controller rather than a real endpoint, so the handler can be
 * verified before any production endpoint exists.
 */
@WebMvcTest(controllers = GlobalExceptionHandlerTest.FailingController.class)
@Import({ GlobalExceptionHandler.class, RequestIdFilter.class,
		GlobalExceptionHandlerTest.FailingController.class })
@WithMockUser
class GlobalExceptionHandlerTest {

	@Autowired
	private MockMvc mockMvc;

	@RestController
	static class FailingController {

		@GetMapping("/test/business-failure")
		void businessFailure() {
			throw new ApiException(ErrorCode.VEHICLE_NOT_FOUND);
		}

		@GetMapping("/test/unexpected-failure")
		void unexpectedFailure() {
			throw new IllegalStateException("connection to 10.0.0.7 refused for user db_admin");
		}
	}

	@Test
	void businessFailureAnswersWithItsOwnCodeAndStatus() throws Exception {
		mockMvc.perform(get("/test/business-failure"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("VEHICLE_NOT_FOUND"))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.path").value("/test/business-failure"))
				.andExpect(jsonPath("$.requestId").isNotEmpty())
				.andExpect(jsonPath("$.fieldErrors").isEmpty());
	}

	/**
	 * Guards specification section 30. The thrown message deliberately contains
	 * an internal host and a database user; neither may appear in the response.
	 */
	@Test
	void unexpectedFailureNeverLeaksInternalDetail() throws Exception {
		mockMvc.perform(get("/test/unexpected-failure"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
				.andExpect(content().string(not(containsString("10.0.0.7"))))
				.andExpect(content().string(not(containsString("db_admin"))))
				.andExpect(content().string(not(containsString("IllegalStateException"))));
	}
}