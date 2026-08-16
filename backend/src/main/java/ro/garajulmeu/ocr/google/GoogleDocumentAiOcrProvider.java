package ro.garajulmeu.ocr.google;

import java.io.IOException;

import com.google.api.gax.rpc.StatusCode;
import com.google.cloud.documentai.v1.DocumentProcessorServiceClient;
import com.google.cloud.documentai.v1.DocumentProcessorServiceSettings;
import com.google.cloud.documentai.v1.ProcessRequest;
import com.google.cloud.documentai.v1.RawDocument;
import com.google.protobuf.ByteString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;
import ro.garajulmeu.ocr.OcrDocument;
import ro.garajulmeu.ocr.OcrImage;
import ro.garajulmeu.ocr.OcrProvider;

/**
 * The section 32 seam, filled in. Everything Document AI knows about stops here.
 *
 * <p>The client is built once and closed at shutdown: it owns a gRPC channel and
 * a thread pool, and building one per request would spend more time on
 * connection setup than on the scan. Building it in the constructor also means a
 * misconfigured deployment <strong>fails to start</strong> rather than answering
 * every scan with a server error - the same choice the provider seam itself
 * makes.
 *
 * <p>Nothing here logs the image or the text read from it, per section 24. What
 * is logged when a call fails is the status code and nothing else.
 */
@Component
@ConditionalOnProperty(name = "garajul-meu.ocr.provider", havingValue = "google")
class GoogleDocumentAiOcrProvider implements OcrProvider, AutoCloseable {

	private static final Logger log = LoggerFactory.getLogger(GoogleDocumentAiOcrProvider.class);

	private final DocumentProcessorServiceClient client;
	private final String processorName;

	GoogleDocumentAiOcrProvider(DocumentAiProperties properties) throws IOException {
		this.processorName = properties.processorName();
		this.client = DocumentProcessorServiceClient.create(
				DocumentProcessorServiceSettings.newBuilder()
						.setEndpoint(properties.endpoint())
						.build());

		log.info("OCR provider is Document AI at {}", properties.endpoint());
	}

	@Override
	public OcrDocument read(OcrImage image) {
		ProcessRequest request = ProcessRequest.newBuilder()
				.setName(processorName)
				.setRawDocument(RawDocument.newBuilder()
						.setContent(ByteString.copyFrom(image.bytes()))
						// The type found in the bytes, never the one the client
						// declared - see OcrImageValidator.
						.setMimeType(image.contentType())
						.build())
				.build();

		try {
			return DocumentAiTranslation.toOcrDocument(
					client.processDocument(request).getDocument());
		} catch (com.google.api.gax.rpc.ApiException failure) {
			throw translated(failure);
		}
	}

	/**
	 * Two outcomes, and the difference is whose fault it is.
	 *
	 * <p>Unavailable, timed out or throttled is the service having a bad moment:
	 * the caller may usefully try again, and it is logged as a warning. Anything
	 * else - a processor that does not exist, a permission we do not have, an
	 * argument it refused - is <strong>our</strong> configuration being wrong, and
	 * it is logged at error precisely so that Sentry picks it up from Phase 15.
	 * The person in front of the screen gets the same unhelpful truth either way,
	 * because there is nothing they can do about it.
	 */
	private ApiException translated(com.google.api.gax.rpc.ApiException failure) {
		StatusCode.Code code = failure.getStatusCode().getCode();

		if (code == StatusCode.Code.UNAVAILABLE
				|| code == StatusCode.Code.DEADLINE_EXCEEDED
				|| code == StatusCode.Code.RESOURCE_EXHAUSTED) {
			log.warn("Document AI is not answering: {}", code);
			return new ApiException(ErrorCode.OCR_PROVIDER_UNAVAILABLE);
		}

		log.error("Document AI refused the request: {}. This is almost always our configuration.", code);
		return new ApiException(ErrorCode.OCR_PROCESSING_FAILED);
	}

	@Override
	public void close() {
		client.close();
	}
}