package ro.garajulmeu.push;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.springframework.stereotype.Component;

/**
 * Makes the encryption invisible above the entity.
 *
 * <p>A converter rather than encrypting in the service, because the token is
 * read in two places that have nothing to do with each other:
 * {@code UserDeviceService} on registration and {@code ReminderDispatcher} when
 * something is actually delivered. Encrypting at the service would leave the
 * dispatcher holding ciphertext and sending it to Firebase - which fails
 * silently, as a token Firebase simply does not recognise.
 *
 * <p>{@code @Component} as well as {@code @Converter}: Spring Boot wires
 * Hibernate's bean container to the application context, so this receives the
 * cipher by constructor injection. If that ever stopped being true the failure
 * would be loud - Hibernate cannot instantiate a class with no no-arg
 * constructor - which is why it is safe to depend on.
 */
@Converter
@Component
public class PushTokenConverter implements AttributeConverter<String, String> {

	private final PushTokenCipher cipher;

	PushTokenConverter(PushTokenCipher cipher) {
		this.cipher = cipher;
	}

	@Override
	public String convertToDatabaseColumn(String plaintext) {
		return plaintext == null ? null : cipher.encrypt(plaintext);
	}

	@Override
	public String convertToEntityAttribute(String stored) {
		return stored == null ? null : cipher.decrypt(stored);
	}
}