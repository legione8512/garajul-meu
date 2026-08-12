package ro.garajulmeu.user;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Stores {@link Language} as its lower-case code rather than the enum name.
 *
 * <p>{@code @Enumerated(STRING)} would write RO and EN, which would break the
 * CHECK constraint in the migration and would not match the language tags the
 * frontend and the email templates use. {@code autoApply} means every Language
 * attribute is converted without repeating the annotation.
 */
@Converter(autoApply = true)
public class LanguageConverter implements AttributeConverter<Language, String> {

	@Override
	public String convertToDatabaseColumn(Language language) {
		return language == null ? null : language.code();
	}

	@Override
	public Language convertToEntityAttribute(String code) {
		return code == null ? null : Language.fromCode(code);
	}
}