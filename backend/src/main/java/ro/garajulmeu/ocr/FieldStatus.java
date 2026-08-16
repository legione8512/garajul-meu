package ro.garajulmeu.ocr;

/**
 * The three states section 7 puts on an overlay field. Colour is never their
 * only expression on screen - that is section 7 as well - which is why this is a
 * name rather than a shade.
 */
public enum FieldStatus {

	/** Read confidently and it makes sense for the kind of field it is. */
	DETECTED,

	/**
	 * Something was read, but either the provider was unsure or the value does
	 * not hold up: a VIN of the wrong length, a date nobody can parse. Section 13
	 * asks for exactly this combination rather than for confidence alone.
	 */
	NEEDS_REVIEW,

	/** Nothing was found for this field, which is most of them on a poor photograph. */
	NOT_DETECTED
}