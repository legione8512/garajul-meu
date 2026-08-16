package ro.garajulmeu.ocr;

import java.io.IOException;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import ro.garajulmeu.exception.ApiException;
import ro.garajulmeu.exception.ErrorCode;

/** The path and verb fixed by the section 16 API contract. */
@RestController
public class OcrController {

	private final OcrService ocrService;

	OcrController(OcrService ocrService) {
		this.ocrService = ocrService;
	}

	/**
	 * Not nested under a vehicle, and that is the contract rather than an
	 * oversight: a scan belongs to nothing until the person has reviewed it and
	 * saved it through the certificate's own endpoint.
	 *
	 * <p>The declared filename and content type of the part are ignored entirely.
	 * {@link OcrImageValidator} reads the format out of the bytes.
	 */
	@PostMapping(path = "/api/v1/ocr/registration-certificate",
			consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public OcrScan scan(@AuthenticationPrincipal Jwt token, @RequestPart("image") MultipartFile image) {
		return ocrService.scan(UUID.fromString(token.getSubject()), bytesOf(image));
	}

	/**
	 * A part that cannot be read is a broken upload, not a broken server. Left
	 * alone it would reach the catch-all handler and answer 500 for something the
	 * caller did.
	 */
	private static byte[] bytesOf(MultipartFile image) {
		try {
			return image.getBytes();
		} catch (IOException exception) {
			throw new ApiException(ErrorCode.OCR_FILE_INVALID);
		}
	}
}