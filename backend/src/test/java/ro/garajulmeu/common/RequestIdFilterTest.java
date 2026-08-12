package ro.garajulmeu.common;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdFilterTest {

	private final RequestIdFilter filter = new RequestIdFilter();

	@Test
	void generatesAnIdentifierWhenTheClientSendsNone() throws Exception {
		AtomicReference<String> seenInsideChain = new AtomicReference<>();
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(new MockHttpServletRequest(), response,
				(req, res) -> seenInsideChain.set(MDC.get(RequestIdFilter.MDC_KEY)));

		assertThat(seenInsideChain.get()).isNotBlank();
		assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo(seenInsideChain.get());
	}

	@Test
	void reusesAnIdentifierTheClientSuppliedSoOneRequestCanBeTracedEndToEnd() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(RequestIdFilter.HEADER, "mobile-42_abc.7");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, (req, res) -> {
		});

		assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo("mobile-42_abc.7");
	}

	@Test
	void replacesAnUnsafeClientIdentifierRatherThanWritingItToTheLog() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(RequestIdFilter.HEADER, "abc\nERROR forged log line");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, (req, res) -> {
		});

		assertThat(response.getHeader(RequestIdFilter.HEADER)).doesNotContain("forged");
	}

	/**
	 * Servlet threads are pooled. A leaked MDC entry would stamp the previous
	 * caller's identifier onto an unrelated user's log lines.
	 */
	@Test
	void clearsTheMdcOnceTheRequestIsFinished() throws Exception {
		filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (req, res) -> {
		});

		assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
	}
}