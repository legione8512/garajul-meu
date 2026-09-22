package ro.garajulmeu.common;

/**
 * The Orientation tag a camera writes into a JPEG, read straight from the bytes.
 *
 * <p><strong>Why this exists at all.</strong> A phone held sideways does not turn
 * its sensor; it stores the picture as the sensor saw it and writes a number
 * saying how to turn it for display. Every viewer honours that number and
 * {@code ImageIO} does not - it hands back the stored pixels - so a thumbnail
 * made without reading it comes out on its side for exactly the photographs
 * people take of their cars.
 *
 * <p><strong>Nine lines of TIFF rather than a metadata library.</strong> One tag
 * is wanted, in the first directory, of a file we have already decoded once.
 * Bringing in an EXIF dependency to read a single {@code short} would be a supply
 * chain for a field that has not changed since 1995.
 *
 * <p><strong>Everything unexpected answers {@link #UPRIGHT}.</strong> A PNG has
 * no EXIF, a photograph edited by a desktop tool often has none left, and a
 * truncated or hostile segment must not throw: the caller is making a thumbnail,
 * and "leave it as it is" is always a defensible answer while an exception is
 * not. Nothing here trusts a declared length past the end of the array.
 */
final class JpegOrientation {

	/** As stored, which is also what an absent or unreadable tag means. */
	static final int UPRIGHT = 1;

	/** Orientations 5 to 8 turn the picture a quarter, so width and height swap. */
	static final int FIRST_TURNED = 5;

	private static final int LAST = 8;

	private JpegOrientation() {
	}

	/**
	 * Walks the JPEG's segment headers looking for the APP1 that begins with
	 * "Exif\0\0", and stops at the start of the compressed data: everything this
	 * reads is a header, never a pixel.
	 */
	static int of(byte[] bytes) {
		if (bytes.length < 4 || unsigned(bytes, 0) != 0xFF || unsigned(bytes, 1) != 0xD8) {
			return UPRIGHT;
		}

		int position = 2;

		while (position + 4 <= bytes.length) {
			if (unsigned(bytes, position) != 0xFF) {
				return UPRIGHT;
			}

			int marker = unsigned(bytes, position + 1);

			// Padding before a marker is legal and carries no length.
			if (marker == 0xFF) {
				position += 1;
				continue;
			}

			// Start of scan, or end of image: the headers are over.
			if (marker == 0xDA || marker == 0xD9) {
				return UPRIGHT;
			}

			int length = (unsigned(bytes, position + 2) << 8) | unsigned(bytes, position + 3);

			if (length < 2 || position + 2 + length > bytes.length) {
				return UPRIGHT;
			}

			if (marker == 0xE1 && isExif(bytes, position + 4)) {
				return inTiff(bytes, position + 10, position + 2 + length);
			}

			position += 2 + length;
		}

		return UPRIGHT;
	}

	private static boolean isExif(byte[] bytes, int start) {
		return start + 6 <= bytes.length
				&& bytes[start] == 'E' && bytes[start + 1] == 'x' && bytes[start + 2] == 'i'
				&& bytes[start + 3] == 'f' && bytes[start + 4] == 0 && bytes[start + 5] == 0;
	}

	/**
	 * The TIFF header inside the APP1 segment: a byte order, a magic 42, and the
	 * offset of the first directory. Offsets are counted from the start of this
	 * header, which is why {@code start} is carried through rather than the
	 * position in the file.
	 */
	private static int inTiff(byte[] bytes, int start, int end) {
		if (start + 8 > end) {
			return UPRIGHT;
		}

		boolean little = bytes[start] == 'I' && bytes[start + 1] == 'I';
		boolean big = bytes[start] == 'M' && bytes[start + 1] == 'M';

		if (!little && !big) {
			return UPRIGHT;
		}

		// Kept in a long until it has been bounded: a four-byte offset from a
		// damaged file is free to be larger than the segment, or than an int.
		long offset = integer(bytes, start + 4, little);

		if (offset < 8 || start + offset + 2 > end) {
			return UPRIGHT;
		}

		int directory = (int) (start + offset);

		int entries = shorts(bytes, directory, little);

		for (int entry = 0; entry < entries; entry++) {
			int at = directory + 2 + entry * 12;

			if (at + 12 > end) {
				return UPRIGHT;
			}
			if (shorts(bytes, at, little) == 0x0112) {
				// A SHORT value is written in the first two bytes of the
				// twelve-byte entry's value field, whichever way round it is.
				int value = shorts(bytes, at + 8, little);
				return value >= UPRIGHT && value <= LAST ? value : UPRIGHT;
			}
		}

		return UPRIGHT;
	}

	private static int shorts(byte[] bytes, int at, boolean little) {
		return little
				? unsigned(bytes, at) | (unsigned(bytes, at + 1) << 8)
				: (unsigned(bytes, at) << 8) | unsigned(bytes, at + 1);
	}

	/** A long, kept in a Java long so a hostile four-byte offset cannot go negative. */
	private static long integer(byte[] bytes, int at, boolean little) {
		long first = shorts(bytes, at, little);
		long second = shorts(bytes, at + 2, little);

		return little ? first | (second << 16) : (first << 16) | second;
	}

	private static int unsigned(byte[] bytes, int at) {
		return bytes[at] & 0xFF;
	}
}
